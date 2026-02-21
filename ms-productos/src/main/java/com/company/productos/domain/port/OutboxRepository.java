package com.company.productos.domain.port;

import com.company.productos.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

public interface OutboxRepository {
    Mono<Void> save(DomainEvent event);
}
