package com.cart.ecom_proj.service;

import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.CustomerOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Test mode only. No live credentials or real charges are accepted. */
@Service
public class TestPaymentService {
 private static final URI ORDERS=URI.create("https://api.razorpay.com/v1/orders");
 private final OrderService orders; private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 private final String keyId,secret;
 public TestPaymentService(OrderService orders,ObjectMapper json,@Value("${RAZORPAY_TEST_KEY_ID:}") String keyId,@Value("${RAZORPAY_TEST_KEY_SECRET:}") String secret){
  this.orders=orders;this.json=json;this.keyId=keyId;this.secret=secret;
 }
 private void configured(){if(!keyId.startsWith("rzp_test_")||secret.isBlank())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Test payment is not configured");}
 private String authorization(){return "Basic "+Base64.getEncoder().encodeToString((keyId+":"+secret).getBytes(StandardCharsets.UTF_8));}
 @Transactional public PaymentStart start(String username,OrderRequest request){
  configured();
  CustomerOrder order=orders.reserve(username,request,true);
  long paise;
  try{paise=order.getTotal().movePointRight(2).longValueExact();}catch(ArithmeticException e){throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid payment amount");}
  if(paise<=0)throw new ApiException(HttpStatus.BAD_REQUEST,"Payment amount must be positive");
  JsonNode gateway=send(HttpRequest.newBuilder(ORDERS).header("Content-Type","application/json").header("Authorization",authorization())
   .POST(HttpRequest.BodyPublishers.ofString(write(Map.of("amount",paise,"currency","INR","receipt","northshop-"+order.getId())))).build());
  String id=gateway.path("id").asText();
  if(id.isBlank())throw new ApiException(HttpStatus.BAD_GATEWAY,"Payment provider did not create an order");
  orders.linkPayment(order.getId(),id);
  return new PaymentStart(order.getId(),id,keyId,paise,"INR");
 }
 public OrderResponse confirm(int id,String username,PaymentConfirmation data){
  configured();CustomerOrder order=orders.pending(id,username);
  if(!data.gatewayOrderId().equals(order.getGatewayOrderId()))throw new ApiException(HttpStatus.BAD_REQUEST,"Payment order mismatch");
  String expected=sign(data.gatewayOrderId()+"|"+data.paymentId());
  if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),data.signature().getBytes(StandardCharsets.US_ASCII)))
   throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid payment signature");
  JsonNode payment=send(HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/payments/"+URLEncoder.encode(data.paymentId(),StandardCharsets.UTF_8)))
   .header("Authorization",authorization()).GET().build());
  if(!"captured".equals(payment.path("status").asText())||!data.gatewayOrderId().equals(payment.path("order_id").asText())
   ||payment.path("amount").asLong(-1)!=order.getTotal().movePointRight(2).longValueExact()
   ||!"INR".equals(payment.path("currency").asText()))throw new ApiException(HttpStatus.CONFLICT,"Payment has not been captured for this order");
  return orders.paid(id,username,data.gatewayOrderId(),data.paymentId());
 }
 public PaymentStart resume(int id,String username){
  configured();CustomerOrder order=orders.pending(id,username);
  if(order.getGatewayOrderId()==null)throw new ApiException(HttpStatus.CONFLICT,"Payment checkout is not ready");
  return new PaymentStart(id,order.getGatewayOrderId(),keyId,order.getTotal().movePointRight(2).longValueExact(),"INR");
 }
 public OrderResponse sync(int id,String username){
  configured();CustomerOrder order=orders.pending(id,username);
  JsonNode payments=send(HttpRequest.newBuilder(URI.create(ORDERS+"/"+URLEncoder.encode(order.getGatewayOrderId(),StandardCharsets.UTF_8)+"/payments"))
   .header("Authorization",authorization()).GET().build()).path("items");
  for(JsonNode payment:payments){
   if("captured".equals(payment.path("status").asText()) && order.getGatewayOrderId().equals(payment.path("order_id").asText())
    && payment.path("amount").asLong(-1)==order.getTotal().movePointRight(2).longValueExact()
    && "INR".equals(payment.path("currency").asText()))
    return orders.paid(id,username,order.getGatewayOrderId(),payment.path("id").asText());
  }
  throw new ApiException(HttpStatus.CONFLICT,"No captured payment for this order yet");
 }
 public OrderResponse abandon(int id,String username){
  configured();CustomerOrder order=orders.pending(id,username);
  JsonNode payments=send(HttpRequest.newBuilder(URI.create(ORDERS+"/"+URLEncoder.encode(order.getGatewayOrderId(),StandardCharsets.UTF_8)+"/payments"))
   .header("Authorization",authorization()).GET().build()).path("items");
  for(JsonNode payment:payments){
   if("captured".equals(payment.path("status").asText())||"authorized".equals(payment.path("status").asText()))
    throw new ApiException(HttpStatus.CONFLICT,"Payment is processing or captured; check its status instead");
  }
  return orders.cancelCheckedPending(id,username);
 }
 private String sign(String input){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Cannot verify payment",e);}}
 private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot encode request",e);}}
 private JsonNode send(HttpRequest req){try{
  HttpResponse<String> result=http.send(req,HttpResponse.BodyHandlers.ofString());
  if(result.statusCode()<200||result.statusCode()>=300)throw new ApiException(HttpStatus.BAD_GATEWAY,"Test payment provider request failed ("+result.statusCode()+")");
  return json.readTree(result.body());
 }catch(ApiException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ApiException(HttpStatus.BAD_GATEWAY,"Test payment provider unavailable");}}
}
