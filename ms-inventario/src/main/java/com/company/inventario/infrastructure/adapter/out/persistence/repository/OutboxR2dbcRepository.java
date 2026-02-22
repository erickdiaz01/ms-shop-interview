package com.company.inventario.infrastructure.adapter.out.persistence.repository;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface OutboxR2dbcRepository extends R2dbcRepository<OutboxEventEntity, UUID> {
    @Query("SELECT * FROM outbox_events WHERE published = false ORDER BY created_at LIMIT 50")
    Flux<OutboxEventEntity> findUnpublished();
    @Query("UPDATE outbox_events SET published = true WHERE id = :id")
    Mono<Void> markPublished(UUID id);
}
