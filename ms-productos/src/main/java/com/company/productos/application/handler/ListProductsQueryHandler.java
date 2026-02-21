package com.company.productos.application.handler;

import com.company.productos.application.command.ListProductsCommand;
import com.company.productos.application.query.PagedResult;
import com.company.productos.application.query.ProductResult;
import com.company.productos.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ListProductsQueryHandler {

    private final ProductRepository productRepository;

    public Mono<PagedResult<ProductResult>> handle(ListProductsCommand command) {
        return Mono.zip(
            productRepository.findAllActive(command.page(), command.size())
                .map(p -> new ProductResult(p.getId().toString(), p.getName(),
                        p.getPrice().amount(), p.getDescription(), p.isActive(),
                        p.getCreatedAt(), p.getUpdatedAt()))
                .collectList(),
            productRepository.countActive()
        ).map(tuple -> new PagedResult<>(tuple.getT1(), tuple.getT2(), command.page(), command.size()));
    }
}
