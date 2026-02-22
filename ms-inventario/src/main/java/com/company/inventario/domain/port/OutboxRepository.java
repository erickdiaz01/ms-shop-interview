package com.company.inventario.domain.port;
import com.company.inventario.domain.event.DomainEvent;
import reactor.core.publisher.Mono;
public interface OutboxRepository {
    Mono<Void> save(DomainEvent event);
}
