package com.company.productos.domain.valueobject;
import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {
    public ProductId { Objects.requireNonNull(value, "ProductId must not be null"); }
    public static ProductId of(String id) { return new ProductId(UUID.fromString(id)); }
    public static ProductId generate()    { return new ProductId(UUID.randomUUID()); }
    @Override public String toString()    { return value.toString(); }
}
