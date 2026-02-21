package com.company.productos.domain.model;

import com.company.productos.domain.event.DomainEvent;
import com.company.productos.domain.event.ProductCreatedEvent;
import com.company.productos.domain.valueobject.Money;
import com.company.productos.domain.valueobject.ProductId;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
@Slf4j
@Getter
public class Product {

    private final ProductId id;
    private String name;
    private Money price;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Product(ProductId id, String name, Money price, String description,
                    boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    /**
     * Factory method — crea un nuevo producto y registra ProductCreatedEvent.
     * El evento incluye precio y descripción para que ms-inventario pueda
     * inicializar el stock SIN necesitar hacer una llamada HTTP adicional.
     */
    public static Product create(String name, Money price, String description) {
        Objects.requireNonNull(name, "Product name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Product name must not be blank");
        Objects.requireNonNull(price, "Price must not be null");

        Instant now = Instant.now();
        Product p = new Product(ProductId.generate(), name.trim(), price, description, true, now, now);

        // Emitir evento enriquecido — ms-inventario lo consumirá para crear stock en 0
        p.domainEvents.add(new ProductCreatedEvent(
                p.id.toString(),
                p.name,
                p.price.amount(),
                p.description,
                0  // minStock por defecto — puede configurarse por request en futuras versiones
        ));
        log.debug("Evento agregado al producto con id: {}", p.id);

        return p;
    }

    public static Product reconstitute(ProductId id, String name, Money price, String description,
                                       boolean active, Instant createdAt, Instant updatedAt) {
        return new Product(id, name, price, description, active, createdAt, updatedAt);
    }


    public void updatePrice(Money newPrice) {
        this.price = Objects.requireNonNull(newPrice);
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public List<DomainEvent> getDomainEvents() { return Collections.unmodifiableList(domainEvents); }

    public void addDomainEvents(List<DomainEvent> events) {
        this.domainEvents.addAll(events);
    }
    public void clearDomainEvents()            { domainEvents.clear(); }
}
