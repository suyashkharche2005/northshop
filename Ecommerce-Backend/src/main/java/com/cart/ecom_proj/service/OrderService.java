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
 @Transactional public OrderResponse place(String username,OrderRequest request){
  User user=users.findByUsername(username).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Sign in again"));
  Map<Integer,Integer> quantities=new TreeMap<>();
  for(OrderRequest.Item item:request.items()){if(item.productId()==null||item.productId()<1)throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid product");quantities.merge(item.productId(),item.quantity(),Integer::sum);}
  List<OrderItem> lines=new ArrayList<>(); BigDecimal total=BigDecimal.ZERO;
  for(var entry:quantities.entrySet()){
   Product product=products.lockById(entry.getKey()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Product not found: "+entry.getKey()));
   int quantity=entry.getValue();if(quantity<1||!product.isProductAvailable()||product.getStockQuantity()<quantity)throw new ApiException(HttpStatus.CONFLICT,"Insufficient stock for "+product.getName());
   lines.add(new OrderItem(product,quantity));total=total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
   product.setStockQuantity(product.getStockQuantity()-quantity);if(product.getStockQuantity()==0)product.setProductAvailable(false);
  }
  CustomerOrder order=new CustomerOrder(user,total);lines.forEach(order::addItem);return view(orders.save(order));
 }
 @Transactional(readOnly=true) public List<OrderResponse> mine(String username){return orders.findByCustomerUsernameOrderByCreatedAtDesc(username).stream().map(this::view).toList();}
 private OrderResponse view(CustomerOrder o){return new OrderResponse(o.getId(),o.getCreatedAt(),o.getTotal(),o.getItems().stream().map(i->new OrderResponse.Line(i.getProduct().getId(),i.getProductName(),i.getUnitPrice(),i.getQuantity())).toList());}
}
