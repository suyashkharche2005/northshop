package com.cart.ecom_proj.controller;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.service.ShoppingAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/assistant")
public class ShoppingAssistantController {
 private final ShoppingAssistantService service;
 public ShoppingAssistantController(ShoppingAssistantService service){this.service=service;}
 @PostMapping("/ask") public ShoppingAnswer ask(@Valid @RequestBody ShoppingQuestion request){return service.ask(request);}
}
