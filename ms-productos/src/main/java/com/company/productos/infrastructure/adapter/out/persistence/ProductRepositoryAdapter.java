package com.company.productos.infrastructure.adapter.out.persistence;

import com.company.productos.domain.model.Product;
import com.company.productos.domain.port.ProductRepository;
import com.company.productos.domain.valueobject.ProductId;
import com.company.productos.infrastructure.adapter.out.persistence.mapper.ProductPersistenceMapper;
import com.company.productos.infrastructure.adapter.out.persistence.repository.ProductR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductR2dbcRepository r2dbcRepository;
    private final ProductPersistenceMapper mapper;

    @Override
    public Mono<Product> findById(ProductId id) {
        return r2dbcRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Mono<Product> findByName(String name) {
        return r2dbcRepository.findByNameAndActiveTrue(name).map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findAllActive(int page, int size) {
        return r2dbcRepository.findAllByActiveTrue(PageRequest.of(page, size)).map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countActive() {
        return r2dbcRepository.countByActiveTrue();
    }

    @Override
    public Mono<Product> save(Product product) {
        return r2dbcRepository.save(mapper.toEntity(product)).map(mapper::toDomain);
    }
}
