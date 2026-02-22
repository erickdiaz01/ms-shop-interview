package com.company.inventario.domain.valueobject;
import java.util.Objects;
import java.util.UUID;
public record InventoryId(UUID value) {
    public InventoryId { Objects.requireNonNull(value); }
    public static InventoryId of(String id) { return new InventoryId(UUID.fromString(id)); }
    public static InventoryId generate()    { return new InventoryId(UUID.randomUUID()); }
    @Override public String toString()      { return value.toString(); }
}
