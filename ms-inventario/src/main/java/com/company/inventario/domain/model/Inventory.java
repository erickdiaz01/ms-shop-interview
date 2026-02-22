package com.company.inventario.domain.model;

import com.company.inventario.domain.event.DomainEvent;
import com.company.inventario.domain.event.InventoryUpdatedEvent;
import com.company.inventario.domain.exception.InsufficientStockException;
import com.company.inventario.domain.valueobject.InventoryId;
import com.company.inventario.domain.valueobject.ProductId;
import com.company.inventario.domain.valueobject.Quantity;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

@Getter
public class Inventory {

    private final InventoryId id;
    private final ProductId productId;
    private Quantity quantity;
    private final int minStock;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private final Long version; // No persistente, solo para infraestructura

    private Inventory(InventoryId id, ProductId productId, Quantity quantity, int minStock,
                      Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.minStock = minStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /** Factory — for new inventory registration */
    public static Inventory create(ProductId productId, int initialQty, int minStock) {
        Objects.requireNonNull(productId);
        if (initialQty < 0) throw new IllegalArgumentException("Initial quantity cannot be negative");
        Instant now = Instant.now();
        return new Inventory(InventoryId.generate(), productId,
                Quantity.of(initialQty), minStock, now, now, null); // nuevo → version null
    }

    /** Reconstitution from persistence */
    public static Inventory reconstitute(InventoryId id, ProductId productId, Quantity quantity,
                                         int minStock, Instant createdAt, Instant updatedAt, Long version) {
        return new Inventory(id, productId, quantity, minStock, createdAt, updatedAt, version);
    }

    /** Core business operation: purchase */
    public void purchase(int amount, String correlationId) {
        if (amount <= 0) throw new IllegalArgumentException("Purchase amount must be positive");
        if (!quantity.isSufficient(amount))
            throw new InsufficientStockException(quantity.value(), amount);

        int prev = quantity.value();
        this.quantity = quantity.subtract(amount);
        this.updatedAt = Instant.now();
        domainEvents.add(new InventoryUpdatedEvent(
                productId.toString(), prev, quantity.value(), "PURCHASE", correlationId));
    }

    /** Update stock manually */
    public void updateStock(int newQty, String correlationId) {
        if (newQty < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        int prev = quantity.value();
        this.quantity = Quantity.of(newQty);
        this.updatedAt = Instant.now();
        domainEvents.add(new InventoryUpdatedEvent(
                productId.toString(), prev, newQty, "ADJUSTMENT", correlationId));
    }

    public boolean isLowStock() {
        return quantity.value() <= minStock;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}