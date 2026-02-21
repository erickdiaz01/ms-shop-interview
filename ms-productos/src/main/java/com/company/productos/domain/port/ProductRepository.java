package com.company.productos.domain.port;
import com.company.productos.domain.model.Product;
import com.company.productos.domain.valueobject.ProductId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepository {
    Mono<Product> findById(ProductId id);
    Mono<Product> findByName(String name);
    Flux<Product> findAllActive(int page, int size);
    Mono<Long>    countActive();
    Mono<Product> save(Product product);
}
