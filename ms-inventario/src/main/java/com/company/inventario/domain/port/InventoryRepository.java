package com.company.inventario.domain.port;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.valueobject.ProductId;
import reactor.core.publisher.Mono;
public interface InventoryRepository {
    Mono<Inventory> findByProductId(ProductId productId);
    Mono<Inventory> save(Inventory inventory);
}
