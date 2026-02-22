package com.company.inventario.domain.event;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class InventoryUpdatedEvent implements DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();
    private final String aggregateId;
    private final String productId;
    private final int previousQuantity;
    private final int newQuantity;
    private final String reason;
    private final String correlationId;

    public InventoryUpdatedEvent(String productId, int previousQuantity, int newQuantity,
                                  String reason, String correlationId) {
        this.aggregateId = productId;
        this.productId = productId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.reason = reason;
        this.correlationId = correlationId;
    }
}
