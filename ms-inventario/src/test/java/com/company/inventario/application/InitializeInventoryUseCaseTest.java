package com.company.inventario.application;

import com.company.inventario.application.usecase.InitializeInventoryUseCase;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.valueobject.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitializeInventoryUseCaseTest {

    @Mock InventoryRepository inventoryRepository;
    @InjectMocks InitializeInventoryUseCase useCase;

    private static final String PRODUCT_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void execute_shouldCreateInventoryWithZeroStock_whenProductIsNew() {
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(PRODUCT_ID, 5))
                .assertNext(inventory -> {
                    assertThat(inventory.getQuantity().value()).isZero();
                    assertThat(inventory.getMinStock()).isEqualTo(5);
                    assertThat(inventory.getProductId().toString()).isEqualTo(PRODUCT_ID);
                })
                .verifyComplete();

        verify(inventoryRepository).save(any());
    }

    @Test
    void execute_shouldBeIdempotent_whenInventoryAlreadyExists() {
        Inventory existing = Inventory.create(ProductId.of(PRODUCT_ID), 10, 2);
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.execute(PRODUCT_ID, 5))
                .assertNext(inventory -> assertThat(inventory.getQuantity().value()).isEqualTo(10))
                .verifyComplete();

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void execute_shouldSetMinStockFromEvent() {
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.execute(PRODUCT_ID, 10))
                .assertNext(inventory -> assertThat(inventory.getMinStock()).isEqualTo(10))
                .verifyComplete();
    }
}
