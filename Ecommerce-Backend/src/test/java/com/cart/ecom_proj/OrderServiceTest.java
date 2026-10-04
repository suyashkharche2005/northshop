package com.cart.ecom_proj;
import com.cart.ecom_proj.dto.OrderRequest;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.*;
import com.cart.ecom_proj.repo.*;
import com.cart.ecom_proj.service.OrderService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderServiceTest {
 @Test void insufficientStockRejectsOrderWithoutSaving() {
  ProductRepo products=mock(ProductRepo.class);UserRepo users=mock(UserRepo.class);OrderRepo orders=mock(OrderRepo.class);
  User user=new User();user.setUsername("buyer");when(users.findByUsername("buyer")).thenReturn(Optional.of(user));
  Product item=new Product();item.setId(1);item.setName("Laptop");item.setPrice(new BigDecimal("500.00"));item.setStockQuantity(1);item.setProductAvailable(true);
  when(products.lockById(1)).thenReturn(Optional.of(item));
  OrderService service=new OrderService(products,users,orders);
  assertThrows(ApiException.class,()->service.place("buyer",new OrderRequest(List.of(new OrderRequest.Item(1,2)))));
  assertEquals(1,item.getStockQuantity());verifyNoInteractions(orders);
 }
}
