package com.company.inventario.domain.port;
import com.company.inventario.domain.model.Purchase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface PurchaseRepository {
    Mono<Purchase> save(Purchase purchase);
    Flux<Purchase> findByProductId(UUID productId, int page, int size);
}
