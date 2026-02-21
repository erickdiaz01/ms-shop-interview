package com.company.productos.infrastructure.adapter.in.rest.mapper;

import com.company.productos.application.query.ProductResult;
import com.company.productos.infrastructure.adapter.in.rest.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductRestMapper {

    public ProductResponse toResponse(ProductResult result) {
        return new ProductResponse(
                result.id(),
                "products",
                new ProductResponse.ProductAttributes(
                        result.name(),
                        result.price(),
                        result.description(),
                        result.active(),
                        result.createdAt(),
                        result.updatedAt()
                )
        );
    }
}
