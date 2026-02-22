package com.company.inventario.infrastructure.adapter.out.kafka;

import com.company.inventario.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.company.inventario.infrastructure.adapter.out.persistence.repository.OutboxR2dbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxR2dbcRepository outboxRepository;

    @Value("${app.kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    @Value("${app.kafka.topics.purchase-completed}")
    private String purchaseCompletedTopic;

    public InventoryEventProducer(
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            OutboxR2dbcRepository outboxRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
    }

    /** Outbox Polling Publisher — runs every 5 seconds */
    @Scheduled(fixedDelayString = "5000")
    public void publishPendingEvents() {
        outboxRepository.findUnpublished()
                .flatMap(this::publishEvent)
                .subscribe(
                        e -> log.debug("Published outbox event: {}", e.getEventType()),
                        err -> log.error("Error publishing outbox event: {}", err.getMessage())
                );
    }

    private Mono<OutboxEventEntity> publishEvent(OutboxEventEntity entity) {
        String topic = entity.getEventType().contains("Purchase")
                ? purchaseCompletedTopic : inventoryEventsTopic;

        return Mono.fromFuture(
                        kafkaTemplate.send(topic, entity.getAggregateId(), entity.getPayload()).toCompletableFuture()
                )
                .then(outboxRepository.markPublished(entity.getId()))
                .thenReturn(entity)
                .doOnError(err -> log.error("Failed to publish event {}: {}", entity.getId(), err.getMessage()));
    }
}