package com.company.productos.infrastructure.adapter.out.kafka;

import com.company.productos.infrastructure.adapter.out.persistence.repository.OutboxR2dbcRepository;
import com.company.productos.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Outbox Polling Publisher — cada 5 segundos lee eventos no publicados
 * de outbox_events y los envía a Kafka.
 * Garantiza que ningún ProductCreatedEvent se pierda aunque Kafka esté caído.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxR2dbcRepository outboxRepository;

    @Value("${app.kafka.topics.product-events:product.events}")
    private String productEventsTopic;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-poll-ms:5000}")
    public void publishPendingEvents() {
        log.debug("Outbox publisher running");

        outboxRepository.findUnpublished()
                .doOnNext(e -> log.debug("Found unpublished event: {}", e.getId()))
                .flatMap(this::publish)
                .subscribe(
                        e -> log.debug("Published: type={} id={}", e.getEventType(), e.getId()),
                        err -> log.error("Outbox publish error: {}", err.getMessage())
                );
    }

    private Mono<OutboxEventEntity> publish(OutboxEventEntity entity) {
        return Mono.fromFuture(
                        kafkaTemplate.send(productEventsTopic, entity.getAggregateId(), entity.getPayload())
                                .toCompletableFuture()
                )
                .then(outboxRepository.markPublished(entity.getId()))
                .thenReturn(entity)
                .doOnError(err -> log.error("Failed to publish outbox event {}: {}",
                        entity.getId(), err.getMessage()));
    }
}
