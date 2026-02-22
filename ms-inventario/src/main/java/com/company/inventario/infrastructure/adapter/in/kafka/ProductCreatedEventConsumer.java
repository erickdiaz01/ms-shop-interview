package com.company.inventario.infrastructure.adapter.in.kafka;

import com.company.inventario.application.usecase.InitializeInventoryUseCase;
import com.company.inventario.infrastructure.adapter.in.kafka.dto.ProductCreatedEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCreatedEventConsumer {

    private final InitializeInventoryUseCase initializeInventoryUseCase;

    @KafkaListener(
            topics = "${app.kafka.topics.product-events:product.events}",
            groupId = "${app.kafka.consumer.group-id:ms-inventario-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProductCreated(ConsumerRecord<String, ProductCreatedEventDto> record, Acknowledgment ack) {
        log.info("Received ProductCreatedEvent: key={} partition={} offset={}",
                record.key(), record.partition(), record.offset());

        ProductCreatedEventDto event = record.value();
        try {
            initializeInventoryUseCase.execute(event.productId(), event.minStock()).block();
            ack.acknowledge();
            log.info("Inventory initialized for productId={}", event.productId());
        } catch (Exception e) {
            log.error("Failed to process ProductCreatedEvent: key={} error={}",
                    record.key(), e.getMessage(), e);
            throw new RuntimeException("Failed to process ProductCreatedEvent", e);
        }
    }
}