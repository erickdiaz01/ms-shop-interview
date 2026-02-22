package com.company.inventario.application.command;
import jakarta.validation.constraints.*;

public record PurchaseCommand(
    @NotBlank String productId,
    @Min(1) int quantity,
    String correlationId
) {}
