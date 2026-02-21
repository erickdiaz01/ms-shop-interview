package com.company.productos.infrastructure.adapter.out.persistence.repository;

import com.company.productos.infrastructure.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ProductR2dbcRepository extends R2dbcRepository<ProductEntity, UUID> {
    Mono<ProductEntity> findByNameAndActiveTrue(String name);
    Flux<ProductEntity> findAllByActiveTrue(Pageable pageable);
    Mono<Long> countByActiveTrue();
}
