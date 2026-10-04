package com.cart.ecom_proj.dto;
public record PaymentStart(int orderId,String gatewayOrderId,String keyId,long amount,String currency) {}
