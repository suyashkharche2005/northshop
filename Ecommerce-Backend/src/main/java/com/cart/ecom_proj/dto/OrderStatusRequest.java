package com.cart.ecom_proj.dto;
import com.cart.ecom_proj.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
public record OrderStatusRequest(@NotNull OrderStatus status) {}
