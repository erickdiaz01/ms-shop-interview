package com.company.inventario.domain.service;

import com.company.inventario.domain.exception.InventoryNotFoundException;
import com.company.inventario.domain.exception.InsufficientStockException;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.model.Purchase;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.port.ProductPort;
import com.company.inventario.domain.port.PurchaseRepository;
import com.company.inventario.domain.port.OutboxRepository;
import com.company.inventario.domain.event.PurchaseCompletedEvent;
import com.company.inventario.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryDomainService {

    private final InventoryRepository inventoryRepository;
    private final PurchaseRepository  purchaseRepository;
    private final OutboxRepository    outboxRepository;
    private final ProductPort         productPort;

    @Transactional
    public Mono<PurchaseResult> processPurchase(String productId, int quantity, String correlationId) {
        log.info("Processing purchase: productId={}, quantity={}, correlationId={}", productId, quantity, correlationId);

        return inventoryRepository.findByProductId(ProductId.of(productId))
                .switchIfEmpty(Mono.error(new InventoryNotFoundException(productId)))
                .flatMap(inventory ->                              // ← solo entra si inventory existe
                        productPort.getProduct(productId)            // ← se llama DESPUÉS, no antes
                                .flatMap(product -> {
                                    BigDecimal unitPrice = product.price();
                                    inventory.purchase(quantity, correlationId);

                                    Purchase purchase = Purchase.create(
                                            inventory.getProductId().value(), quantity, unitPrice, correlationId);

                                    PurchaseCompletedEvent event = new PurchaseCompletedEvent(
                                            productId, quantity, unitPrice.multiply(BigDecimal.valueOf(quantity)), correlationId);

                                    return inventoryRepository.save(inventory)
                                            .then(purchaseRepository.save(purchase))
                                            .then(outboxRepository.save(event))
                                            .thenReturn(new PurchaseResult(
                                                    purchase.getId().toString(),
                                                    productId,
                                                    product.name(),
                                                    quantity,
                                                    unitPrice,
                                                    purchase.getTotalAmount(),
                                                    inventory.getQuantity().value()
                                            ));
                                })
                );
    }

    public record PurchaseResult(
            String purchaseId,
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            int remainingStock
    ) {}
}
