package com.company.inventario.infrastructure;

import com.company.inventario.application.usecase.InitializeInventoryUseCase;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.valueobject.ProductId;
import com.company.inventario.infrastructure.adapter.in.kafka.ProductCreatedEventConsumer;
import com.company.inventario.infrastructure.adapter.in.kafka.dto.ProductCreatedEventDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCreatedEventConsumerTest {

    @Mock
    InitializeInventoryUseCase initializeInventoryUseCase;
    @Mock
    Acknowledgment acknowledgment;

    private ProductCreatedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // El listener ahora solo necesita el caso de uso
        consumer = new ProductCreatedEventConsumer(initializeInventoryUseCase);
    }

    @Test
    void onProductCreated_shouldInitializeInventoryAndAcknowledge() {
        String productId = "00000000-0000-0000-0000-000000000001";
        String eventId = "evt-1";
        Instant occurredAt = Instant.parse("2026-02-19T10:00:00Z");

        ProductCreatedEventDto event = new ProductCreatedEventDto(
                eventId,
                productId,
                "Laptop",
                new BigDecimal("999.99"),
                "Gaming laptop",
                5,
                occurredAt
        );

        Inventory fakeInventory = Inventory.create(ProductId.of(productId), 0, 5);
        when(initializeInventoryUseCase.execute(eq(productId), eq(5)))
                .thenReturn(Mono.just(fakeInventory));

        ConsumerRecord<String, ProductCreatedEventDto> record =
                new ConsumerRecord<>("product.events", 0, 0L, productId, event);

        consumer.onProductCreated(record, acknowledgment);

        verify(initializeInventoryUseCase).execute(productId, 5);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onProductCreated_shouldNotAcknowledge_whenInitializationFails() {
        String productId = "00000000-0000-0000-0000-000000000002";
        ProductCreatedEventDto event = new ProductCreatedEventDto(
                "evt-2",
                productId,
                "X",
                new BigDecimal("10.0"),
                "Description",
                0,
                Instant.now()
        );

        when(initializeInventoryUseCase.execute(anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        ConsumerRecord<String, ProductCreatedEventDto> record =
                new ConsumerRecord<>("product.events", 0, 1L, productId, event);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> consumer.onProductCreated(record, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
    }
}