package com.company.inventario.application.query;
import java.time.Instant;
public record InventoryResult(
    String inventoryId,
    String productId,
    String productName,
    String productDescription,
    java.math.BigDecimal productPrice,
    int quantity,
    int minStock,
    boolean lowStock,
    Instant updatedAt
) {}
