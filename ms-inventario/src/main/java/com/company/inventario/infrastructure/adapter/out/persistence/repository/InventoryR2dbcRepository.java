package com.company.inventario.infrastructure.adapter.out.persistence.repository;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.InventoryEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface InventoryR2dbcRepository extends R2dbcRepository<InventoryEntity, UUID> {
    Mono<InventoryEntity> findByProductId(UUID productId);
}
