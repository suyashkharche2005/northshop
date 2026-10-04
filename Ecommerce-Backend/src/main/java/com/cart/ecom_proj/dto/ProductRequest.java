package com.cart.ecom_proj.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ProductRequest(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=2000) String description, @NotBlank @Size(max=120) String brand,
 @NotNull @DecimalMin("0.01") BigDecimal price, @NotBlank @Size(max=120) String category,
 LocalDate releaseDate, boolean productAvailable, @Min(0) int stockQuantity) {}
