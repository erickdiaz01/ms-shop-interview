package com.company.productos.infrastructure.adapter.out.persistence;

import com.company.productos.domain.event.DomainEvent;
import com.company.productos.domain.port.OutboxRepository;
import com.company.productos.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRepositoryAdapter implements OutboxRepository {

    private final R2dbcEntityTemplate entityTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> save(DomainEvent event) {
        log.debug("Saving outbox event: {}", event);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(payload -> {
                    OutboxEventEntity entity = OutboxEventEntity.builder()
                            .id(UUID.randomUUID())
                            .aggregateId(event.getEventId())
                            .eventType(event.getClass().getSimpleName())
                            .payload(payload)
                            .published(false)
                            .build();
                    // Insert explícito con R2dbcEntityTemplate
                    return entityTemplate.insert(entity);
                })
                .doOnSuccess(e -> log.debug("Outbox event saved: type={}, aggregateId={}",
                        e.getEventType(), e.getAggregateId()))
                .then();
    }
}