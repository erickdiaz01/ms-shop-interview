package com.company.inventario.infrastructure.adapter.out.persistence.mapper;

import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.valueobject.*;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.InventoryEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryPersistenceMapper {

    public Inventory toDomain(InventoryEntity e) {
        return Inventory.reconstitute(
                InventoryId.of(e.getId().toString()),
                ProductId.of(e.getProductId().toString()),
                Quantity.of(e.getQuantity()),
                e.getMinStock(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion() // puede ser null (nuevo) o un valor
        );
    }

    public InventoryEntity toEntity(Inventory d) {
        return InventoryEntity.builder()
                .id(d.getId().value())
                .productId(d.getProductId().value())
                .quantity(d.getQuantity().value())
                .minStock(d.getMinStock())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .version(d.getVersion()) // se pasa el valor técnico (null para nuevos, el valor actual para existentes)
                .build();
    }
}