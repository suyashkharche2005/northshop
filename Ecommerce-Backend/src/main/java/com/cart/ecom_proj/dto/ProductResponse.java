package com.cart.ecom_proj.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ProductResponse(int id, String name, String description, String brand, BigDecimal price,
 String category, LocalDate releaseDate, boolean productAvailable, int stockQuantity,
 String imageName, boolean hasImage) {}
