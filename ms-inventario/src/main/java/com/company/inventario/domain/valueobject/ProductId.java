package com.company.inventario.domain.valueobject;
import java.util.Objects;
import java.util.UUID;
public record ProductId(UUID value) {
    public ProductId { Objects.requireNonNull(value); }
    public static ProductId of(String id) { return new ProductId(UUID.fromString(id)); }
    @Override public String toString()    { return value.toString(); }
}
