package com.company.inventario.application.usecase;

import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Caso de uso: inicializar inventario cuando se crea un producto.
 *
 * Idempotente por diseño: si el productId ya tiene inventario (por re-procesamiento
 * del evento Kafka), simplemente lo ignora sin lanzar error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializeInventoryUseCase {

    private final InventoryRepository inventoryRepository;

    public Mono<Inventory> execute(String productId, int minStock) {
        log.info("Initializing inventory for productId={} minStock={}", productId, minStock);

        return inventoryRepository.findByProductId(ProductId.of(productId))
                .flatMap(existing -> {
                    log.warn("Inventory already exists for productId={}, skipping initialization", productId);
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Inventory inventory = Inventory.create(ProductId.of(productId), 0, minStock);
                    return inventoryRepository.save(inventory)
                            .doOnSuccess(inv -> log.info(
                                    "Inventory initialized: productId={}, quantity=0, minStock={}",
                                    productId, minStock));
                }));
    }
}