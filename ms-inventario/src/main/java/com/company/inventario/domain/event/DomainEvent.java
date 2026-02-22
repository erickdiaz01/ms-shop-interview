package com.company.inventario.domain.event;
import java.time.Instant;
public interface DomainEvent {
    String getEventId();
    String getAggregateId();
    Instant getOccurredAt();
}
