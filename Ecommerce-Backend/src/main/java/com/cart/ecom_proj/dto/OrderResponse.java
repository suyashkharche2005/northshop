package com.cart.ecom_proj.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record OrderResponse(int id, Instant createdAt, BigDecimal total, List<Line> items) {
 public record Line(int productId, String name, BigDecimal unitPrice, int quantity) {}
}
