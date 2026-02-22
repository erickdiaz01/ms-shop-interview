package com.company.inventario.infrastructure.adapter.in.rest.dto;
import java.math.BigDecimal;
import java.time.Instant;

public record InventoryResponse(
    String id, String type, InventoryAttributes attributes
) {
    public record InventoryAttributes(
        String productId, String productName, String productDescription,
        BigDecimal productPrice, int quantity, int minStock, boolean lowStock, Instant updatedAt
    ) {}
}
