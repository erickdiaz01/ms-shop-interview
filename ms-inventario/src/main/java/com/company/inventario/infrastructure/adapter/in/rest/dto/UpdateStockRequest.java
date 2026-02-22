package com.company.inventario.infrastructure.adapter.in.rest.dto;
import jakarta.validation.constraints.*;
public record UpdateStockRequest(
    @Min(value = 0, message = "quantity must be >= 0") int quantity,
    String correlationId
) {}
