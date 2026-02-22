package com.company.inventario.infrastructure.adapter.out.persistence;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.port.InventoryRepository;
import com.company.inventario.domain.valueobject.ProductId;
import com.company.inventario.infrastructure.adapter.out.persistence.mapper.InventoryPersistenceMapper;
import com.company.inventario.infrastructure.adapter.out.persistence.repository.InventoryR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component @RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {
    private final InventoryR2dbcRepository r2dbcRepository;
    private final InventoryPersistenceMapper mapper;

    @Override
    public Mono<Inventory> findByProductId(ProductId productId) {
        return r2dbcRepository.findByProductId(productId.value()).map(mapper::toDomain);
    }
    @Override
    public Mono<Inventory> save(Inventory inventory) {
        return r2dbcRepository.save(mapper.toEntity(inventory)).map(mapper::toDomain);
    }
}
