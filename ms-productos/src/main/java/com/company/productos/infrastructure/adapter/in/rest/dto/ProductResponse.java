package com.company.productos.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    String id,
    String type,
    ProductAttributes attributes
) {
    public record ProductAttributes(
        String name,
        BigDecimal price,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
