package com.cart.ecom_proj;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.*;
import com.cart.ecom_proj.repo.*;
import com.cart.ecom_proj.service.OrderService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderLifecycleTest {
 private final ProductRepo products=mock(ProductRepo.class);
 private final UserRepo users=mock(UserRepo.class);
 private final OrderRepo orders=mock(OrderRepo.class);
 private final OrderService service=new OrderService(products,users,orders);
 private CustomerOrder sample(){
  User user=new User();user.setUsername("buyer");
  Product product=new Product();product.setId(7);product.setName("Mug");product.setPrice(new BigDecimal("150"));product.setStockQuantity(3);
  CustomerOrder order=new CustomerOrder(user,new BigDecimal("300"));order.addItem(new OrderItem(product,2));
  when(orders.lockById(42)).thenReturn(Optional.of(order));
  when(products.lockById(7)).thenReturn(Optional.of(product));
  return order;
 }
 @Test void cancellationRestoresStockOnceAndProtectsHistory(){
  CustomerOrder order=sample();Product product=order.getItems().get(0).getProduct();
  assertEquals("CANCELLED",service.cancel(42,"buyer").status());
  assertEquals(5,product.getStockQuantity());
  service.cancel(42,"buyer");assertEquals(5,product.getStockQuantity());
  assertThrows(ApiException.class,()->service.updateStatus(42,OrderStatus.SHIPPED));
 }
 @Test void otherCustomerCannotCancel(){sample();assertThrows(ApiException.class,()->service.cancel(42,"another"));verifyNoInteractions(products);}
 @Test void paidOrderCannotBeCancelled(){
  CustomerOrder order=sample();order.setGatewayOrderId("order_test");order.setGatewayPaymentId("pay_test");
  assertThrows(ApiException.class,()->service.cancel(42,"buyer"));
  assertEquals(3,order.getItems().get(0).getProduct().getStockQuantity());
 }
 @Test void onlyValidAdminTransitions(){
  CustomerOrder order=sample();
  assertThrows(ApiException.class,()->service.updateStatus(42,OrderStatus.DELIVERED));
  assertEquals("SHIPPED",service.updateStatus(42,OrderStatus.SHIPPED).status());
  assertEquals("DELIVERED",service.updateStatus(42,OrderStatus.DELIVERED).status());
  assertThrows(ApiException.class,()->service.updateStatus(42,OrderStatus.SHIPPED));
 }
}
