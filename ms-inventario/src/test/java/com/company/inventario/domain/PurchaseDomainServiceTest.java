package com.company.inventario.domain;

import com.company.inventario.domain.exception.InsufficientStockException;
import com.company.inventario.domain.exception.InventoryNotFoundException;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.port.*;
import com.company.inventario.domain.service.InventoryDomainService;
import com.company.inventario.domain.valueobject.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseDomainServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock PurchaseRepository   purchaseRepository;
    @Mock OutboxRepository     outboxRepository;
    @Mock ProductPort          productPort;
    @InjectMocks InventoryDomainService domainService;

    @Test
    void processPurchase_shouldSucceed_whenStockIsSufficient() {
        var inv = Inventory.create(ProductId.of("00000000-0000-0000-0000-000000000001"), 10, 2);
        var product = new ProductPort.ProductInfo("00000000-0000-0000-0000-000000000001",
                "Laptop", BigDecimal.valueOf(999.99), "A laptop", true);

        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.just(inv));
        when(productPort.getProduct(any())).thenReturn(Mono.just(product));
        when(inventoryRepository.save(any())).thenAnswer(a -> Mono.just(a.getArgument(0)));
        when(purchaseRepository.save(any())).thenAnswer(a -> Mono.just(a.getArgument(0)));
        when(outboxRepository.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(domainService.processPurchase(
                "00000000-0000-0000-0000-000000000001", 3, "test-corr"))
                .assertNext(r -> {
                    assert r.quantity() == 3;
                    assert r.remainingStock() == 7;
                    assert r.totalAmount().compareTo(BigDecimal.valueOf(2999.97)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void processPurchase_shouldFail_whenInventoryNotFound() {
        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.empty());

        StepVerifier.create(domainService.processPurchase("nonexistent", 1, "corr"))
                .expectError(InventoryNotFoundException.class)
                .verify();
    }

    @Test
    void processPurchase_shouldFail_whenInsufficientStock() {
        var inv = Inventory.create(ProductId.of("00000000-0000-0000-0000-000000000001"), 2, 0);
        var product = new ProductPort.ProductInfo("id", "P", BigDecimal.TEN, null, true);

        when(inventoryRepository.findByProductId(any())).thenReturn(Mono.just(inv));
        when(productPort.getProduct(any())).thenReturn(Mono.just(product));

        StepVerifier.create(domainService.processPurchase(
                "00000000-0000-0000-0000-000000000001", 10, "corr"))
                .expectError(InsufficientStockException.class)
                .verify();
    }
}
