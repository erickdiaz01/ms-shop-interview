package com.company.productos.infrastructure.adapter.out.persistence.mapper;

import com.company.productos.domain.model.Product;
import com.company.productos.domain.valueobject.Money;
import com.company.productos.domain.valueobject.ProductId;
import com.company.productos.infrastructure.adapter.out.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public Product toDomain(ProductEntity entity) {
        if (entity.getId() == null) throw new AssertionError();
        return Product.reconstitute(
                ProductId.of(entity.getId().toString()),
                entity.getName(),
                Money.of(entity.getPrice()),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(Product product) {
        return ProductEntity.builder()
                .id(product.getId().value())
                .name(product.getName())
                .price(product.getPrice().amount())
                .description(product.getDescription())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .version(null)  // Importante: null para isNew() = true
                .build();
    }
}
