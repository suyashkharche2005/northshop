package com.cart.ecom_proj.dto;
import jakarta.validation.constraints.*;
public record ShoppingQuestion(@NotBlank @Size(max=300) String question) {}
