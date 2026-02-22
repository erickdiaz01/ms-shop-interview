package com.company.inventario.infrastructure.adapter.in.rest.dto;
import jakarta.validation.constraints.*;
public record PurchaseRequest(
    @NotBlank(message = "productId is required") String productId,
    @Min(value = 1, message = "quantity must be at least 1") int quantity,
    String correlationId
) {}
