package com.company.inventario.domain.event;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class PurchaseCompletedEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();
    private final String aggregateId;
    private final String productId;
    private final int quantity;
    private final BigDecimal totalAmount;
    private final String correlationId;

    public PurchaseCompletedEvent(String productId, int quantity, BigDecimal totalAmount, String correlationId) {
        this.aggregateId = productId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.correlationId = correlationId;
    }
}
