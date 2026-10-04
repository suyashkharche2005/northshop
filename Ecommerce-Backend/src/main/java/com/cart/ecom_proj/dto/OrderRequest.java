package com.cart.ecom_proj.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record OrderRequest(@NotEmpty List<@Valid Item> items) {
 public record Item(@NotNull Integer productId, @Min(1) int quantity) {}
}
