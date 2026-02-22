package com.company.inventario.domain.model;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Purchase {

    private final UUID id;
    private final UUID productId;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final String correlationId;
    private final Instant purchasedAt;

    private Purchase(UUID id, UUID productId, int quantity, BigDecimal unitPrice,
                     BigDecimal totalAmount, String correlationId, Instant purchasedAt) {
        this.id = id; this.productId = productId; this.quantity = quantity;
        this.unitPrice = unitPrice; this.totalAmount = totalAmount;
        this.correlationId = correlationId; this.purchasedAt = purchasedAt;
    }

    public static Purchase create(UUID productId, int quantity, BigDecimal unitPrice, String correlationId) {
        Objects.requireNonNull(productId);
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Objects.requireNonNull(unitPrice);
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new Purchase(UUID.randomUUID(), productId, quantity, unitPrice, total,
                correlationId, Instant.now());
    }

    public static Purchase reconstitute(UUID id, UUID productId, int quantity, BigDecimal unitPrice,
                                         BigDecimal totalAmount, String correlationId, Instant purchasedAt) {
        return new Purchase(id, productId, quantity, unitPrice, totalAmount, correlationId, purchasedAt);
    }
}
