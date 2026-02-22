package com.company.inventario.infrastructure.adapter.out.persistence;

import com.company.inventario.domain.model.Purchase;
import com.company.inventario.domain.port.PurchaseRepository;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.PurchaseEntity;
import com.company.inventario.infrastructure.adapter.out.persistence.mapper.PurchasePersistenceMapper;
import com.company.inventario.infrastructure.adapter.out.persistence.repository.PurchaseR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseRepositoryAdapter implements PurchaseRepository {

    private final R2dbcEntityTemplate entityTemplate;      // Para forzar INSERT
    private final PurchaseR2dbcRepository r2dbcRepository; // Para consultas
    private final PurchasePersistenceMapper mapper;

    @Override
    public Mono<Purchase> save(Purchase purchase) {
        PurchaseEntity entity = mapper.toEntity(purchase);
        return entityTemplate.insert(entity)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Purchase> findByProductId(UUID productId, int page, int size) {
        return r2dbcRepository.findAllByProductId(productId, PageRequest.of(page, size))
                .map(mapper::toDomain);
    }
}