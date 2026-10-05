package com.cart.ecom_proj.service;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.*;
import com.cart.ecom_proj.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
@Service
public class OrderService {
 private final ProductRepo products; private final UserRepo users; private final OrderRepo orders;
 public OrderService(ProductRepo products,UserRepo users,OrderRepo orders){this.products=products;this.users=users;this.orders=orders;}
 @Transactional public OrderResponse place(String username,OrderRequest request){return view(reserve(username,request,false));}
 @Transactional public CustomerOrder reserve(String username,OrderRequest request,boolean payment){
  User user=users.findByUsername(username).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Sign in again"));
  Map<Integer,Integer> quantities=new TreeMap<>();
  for(OrderRequest.Item item:request.items()){
   if(item.productId()==null||item.productId()<1)throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid product");
   quantities.merge(item.productId(),item.quantity(),(a,b)->{try{return Math.addExact(a,b);}catch(ArithmeticException e){throw new ApiException(HttpStatus.BAD_REQUEST,"Quantity too large");}});
  }
  List<OrderItem> lines=new ArrayList<>(); BigDecimal total=BigDecimal.ZERO;
  for(var entry:quantities.entrySet()){
   Product product=products.lockById(entry.getKey()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Product not found: "+entry.getKey()));
   int quantity=entry.getValue();if(quantity<1||!product.isProductAvailable()||product.getStockQuantity()<quantity)throw new ApiException(HttpStatus.CONFLICT,"Insufficient stock for "+product.getName());
   lines.add(new OrderItem(product,quantity));total=total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
   product.setStockQuantity(product.getStockQuantity()-quantity);if(product.getStockQuantity()==0)product.setProductAvailable(false);
  }
  CustomerOrder order=new CustomerOrder(user,total);if(payment)order.setStatus(OrderStatus.PENDING_PAYMENT);
  lines.forEach(order::addItem);return orders.save(order);
 }
 @Transactional public OrderResponse cancel(int id,String username){return cancel(id,username,false);}
 @Transactional public OrderResponse cancelCheckedPending(int id,String username){return cancel(id,username,true);}
 private OrderResponse cancel(int id,String username,boolean checkedPending){
  CustomerOrder order=locked(id);if(!order.getCustomer().getUsername().equals(username))throw new ApiException(HttpStatus.NOT_FOUND,"Order not found");
  if(order.getStatus()==OrderStatus.CANCELLED)return view(order);
  if(order.getStatus()!=OrderStatus.PLACED && order.getStatus()!=OrderStatus.PENDING_PAYMENT)throw new ApiException(HttpStatus.CONFLICT,"This order can no longer be cancelled");
  if(order.getStatus()==OrderStatus.PENDING_PAYMENT && order.getGatewayOrderId()!=null && !checkedPending)throw new ApiException(HttpStatus.CONFLICT,"Check payment status before cancelling a started payment");
  if(order.getGatewayPaymentId()!=null)throw new ApiException(HttpStatus.CONFLICT,"A paid order needs a refund before cancellation");
  restore(order);order.setStatus(OrderStatus.CANCELLED);return view(order);
 }
 @Transactional public OrderResponse updateStatus(int id,OrderStatus next){
  CustomerOrder order=locked(id);OrderStatus current=order.getStatus();
  if(!(current==OrderStatus.PLACED && next==OrderStatus.SHIPPED || current==OrderStatus.SHIPPED && next==OrderStatus.DELIVERED))
   throw new ApiException(HttpStatus.CONFLICT,"Invalid order status transition");
  order.setStatus(next);return view(order);
 }
 @Transactional(readOnly=true) public List<OrderResponse> mine(String username){return orders.findByCustomerUsernameOrderByCreatedAtDesc(username).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public List<OrderResponse> all(){return orders.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();}
 @Transactional public OrderResponse paid(int id,String username,String gatewayOrderId,String paymentId){
  CustomerOrder order=locked(id);
  if(!order.getCustomer().getUsername().equals(username)||!Objects.equals(order.getGatewayOrderId(),gatewayOrderId))throw new ApiException(HttpStatus.NOT_FOUND,"Payment order not found");
  if(order.getStatus()==OrderStatus.PLACED && paymentId.equals(order.getGatewayPaymentId()))return view(order);
  if(order.getStatus()!=OrderStatus.PENDING_PAYMENT)throw new ApiException(HttpStatus.CONFLICT,"Order is not awaiting payment");
  order.setGatewayPaymentId(paymentId);order.setStatus(OrderStatus.PLACED);return view(order);
 }
 @Transactional public void linkPayment(int id,String gatewayId){CustomerOrder order=locked(id);if(order.getStatus()!=OrderStatus.PENDING_PAYMENT)throw new ApiException(HttpStatus.CONFLICT,"Payment order is no longer pending");order.setGatewayOrderId(gatewayId);}
 @Transactional(readOnly=true) public CustomerOrder pending(int id,String username){CustomerOrder order=orders.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Order not found"));if(!order.getCustomer().getUsername().equals(username)||order.getStatus()!=OrderStatus.PENDING_PAYMENT)throw new ApiException(HttpStatus.CONFLICT,"Order is not awaiting payment");return order;}
 private CustomerOrder locked(int id){return orders.lockById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Order not found"));}
 private void restore(CustomerOrder order){
  order.getItems().stream().sorted(Comparator.comparingInt(OrderItem::getProductId)).forEach(item->{
   Product product=products.lockById(item.getProductId()).orElseGet(()->products.findById(item.getProductId()).orElseThrow(()->new ApiException(HttpStatus.CONFLICT,"Order product missing")));
   product.setStockQuantity(Math.addExact(product.getStockQuantity(),item.getQuantity()));
   if(!Boolean.TRUE.equals(product.getArchived()))product.setProductAvailable(true);
  });
 }
 public OrderResponse view(CustomerOrder o){return new OrderResponse(o.getId(),o.getCreatedAt(),o.getTotal(),o.getStatus().name(),o.getGatewayOrderId()==null?"NOT_REQUIRED":o.getGatewayPaymentId()==null?"PENDING":"PAID",o.getItems().stream().map(i->new OrderResponse.Line(i.getProduct().getId(),i.getProductName(),i.getUnitPrice(),i.getQuantity())).toList());}
}
