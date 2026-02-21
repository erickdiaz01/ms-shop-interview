package com.company.productos.application.handler;

import com.company.productos.application.command.GetProductCommand;
import com.company.productos.application.port.CommandHandler;
import com.company.productos.application.query.ProductResult;
import com.company.productos.domain.exception.ProductNotFoundException;
import com.company.productos.domain.port.ProductRepository;
import com.company.productos.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductQueryHandler implements CommandHandler<GetProductCommand, ProductResult> {

    private final ProductRepository productRepository;

    @Override
    public Mono<ProductResult> handle(GetProductCommand command) {
        log.debug("Fetching product: {}", command.productId());
        return productRepository.findById(ProductId.of(command.productId()))
                .switchIfEmpty(Mono.error(new ProductNotFoundException(command.productId())))
                .map(product -> new ProductResult(
                        product.getId().toString(),
                        product.getName(),
                        product.getPrice().amount(),
                        product.getDescription(),
                        product.isActive(),
                        product.getCreatedAt(),
                        product.getUpdatedAt()
                ));
    }
}
