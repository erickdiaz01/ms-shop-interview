package com.company.inventario.domain.port;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

/** Anti-Corruption Layer: outbound port toward ms-productos */
public interface ProductPort {
    Mono<ProductInfo> getProduct(String productId);

    record ProductInfo(String id, String name, BigDecimal price, String description, boolean active) {}
}
