package com.company.productos.infrastructure.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String name;
    private BigDecimal price;
    private String description;
    private boolean active;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return version == null; }
}
