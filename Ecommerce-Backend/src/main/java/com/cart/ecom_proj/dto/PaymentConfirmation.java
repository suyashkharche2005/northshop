package com.cart.ecom_proj.dto;
import jakarta.validation.constraints.NotBlank;
public record PaymentConfirmation(@NotBlank String gatewayOrderId,@NotBlank String paymentId,@NotBlank String signature) {}
