package com.company.productos.application.query;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResult(
    String id,
    String name,
    BigDecimal price,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
