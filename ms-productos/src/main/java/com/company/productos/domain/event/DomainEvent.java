package com.company.productos.domain.event;
import java.time.Instant;
public interface DomainEvent {
    String getEventId();
    Instant getOccurredAt();
}
