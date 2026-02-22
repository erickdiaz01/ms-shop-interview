package com.company.inventario.infrastructure.adapter.out.persistence.repository;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.PurchaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;
public interface PurchaseR2dbcRepository extends R2dbcRepository<PurchaseEntity, UUID> {
    Flux<PurchaseEntity> findAllByProductId(UUID productId, Pageable pageable);
}
