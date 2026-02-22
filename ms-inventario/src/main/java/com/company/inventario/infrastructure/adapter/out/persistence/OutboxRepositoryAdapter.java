package com.company.inventario.infrastructure.adapter.out.persistence;

import com.company.inventario.domain.event.DomainEvent;
import com.company.inventario.domain.port.OutboxRepository;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Instant;
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
                            .aggregateId(event.getAggregateId())
                            .eventType(event.getClass().getSimpleName())
                            .payload(payload)
                            .published(false)
                            .createdAt(Instant.now())
                            .build();
                    return entityTemplate.insert(entity);
                })
                .doOnSuccess(e -> log.debug("Outbox event saved: type={}, aggregateId={}",
                        e.getEventType(), e.getAggregateId()))
                .then();
    }
}