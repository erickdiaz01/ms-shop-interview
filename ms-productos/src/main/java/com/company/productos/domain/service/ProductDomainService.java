package com.company.productos.domain.service;

import com.company.productos.domain.event.DomainEvent;
import com.company.productos.domain.exception.DuplicateProductException;
import com.company.productos.domain.model.Product;
import com.company.productos.domain.port.ProductRepository;
import com.company.productos.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDomainService {

    private final ProductRepository productRepository;

    public Mono<Product> createProduct(String name, Money price, String description) {
        log.debug("Creating product with name: {}", name);
        return productRepository.findByName(name)
                .flatMap(existing -> Mono.<Product>error(new DuplicateProductException(name)))
                .switchIfEmpty(Mono.defer(() -> {
                    Product product = Product.create(name, price, description);
                    // Copiar eventos antes de guardar
                    List<DomainEvent> events = new ArrayList<>(product.getDomainEvents());
                    return productRepository.save(product)
                            .map(savedProduct -> {
                                // Restaurar eventos en la instancia guardada
                                savedProduct.addDomainEvents(events);
                                log.debug("Eventos restaurados en producto guardado: {}", savedProduct.getId());
                                return savedProduct;
                            });
                }));
    }
}