package com.company.inventario.infrastructure.adapter.in.rest.dto;
import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseResponse(
    String id, String type, PurchaseAttributes attributes
) {
    public record PurchaseAttributes(
        String productId, String productName, int quantity,
        BigDecimal unitPrice, BigDecimal totalAmount, int remainingStock, Instant purchasedAt
    ) {}
}
