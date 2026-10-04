package com.cart.ecom_proj.controller;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.service.TestPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/payments/test")
public class PaymentController {
 private final TestPaymentService payments;
 public PaymentController(TestPaymentService payments){this.payments=payments;}
 @PostMapping("/start") public ResponseEntity<PaymentStart> start(Authentication auth,@Valid @RequestBody OrderRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(payments.start(auth.getName(),request));}
 @PostMapping("/{id}/confirm") public OrderResponse confirm(Authentication auth,@PathVariable int id,@Valid @RequestBody PaymentConfirmation request){return payments.confirm(id,auth.getName(),request);}
 @GetMapping("/{id}") public PaymentStart resume(Authentication auth,@PathVariable int id){return payments.resume(id,auth.getName());}
 @PostMapping("/{id}/sync") public OrderResponse sync(Authentication auth,@PathVariable int id){return payments.sync(id,auth.getName());}
 @PostMapping("/{id}/abandon") public OrderResponse abandon(Authentication auth,@PathVariable int id){return payments.abandon(id,auth.getName());}
}
