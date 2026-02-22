package com.company.inventario.infrastructure.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Table("inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID productId;
    private int quantity;
    private int minStock;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return version == null;
    }
}