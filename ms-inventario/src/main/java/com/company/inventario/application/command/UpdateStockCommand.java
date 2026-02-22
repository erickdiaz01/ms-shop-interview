package com.company.inventario.application.command;
import jakarta.validation.constraints.*;

public record UpdateStockCommand(
    @NotBlank String productId,
    @Min(0) int quantity,
    String correlationId
) {}
