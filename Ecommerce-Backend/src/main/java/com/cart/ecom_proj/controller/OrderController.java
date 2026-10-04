package com.cart.ecom_proj.controller;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/orders")
public class OrderController {
 private final OrderService service; public OrderController(OrderService service){this.service=service;}
 @PostMapping public ResponseEntity<OrderResponse> place(Authentication auth,@Valid @RequestBody OrderRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.place(auth.getName(),request));}
 @GetMapping("/mine") public List<OrderResponse> mine(Authentication auth){return service.mine(auth.getName());}
}
