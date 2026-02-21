package com.company.productos.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio publicado cuando se crea un producto.
 * ms-inventario lo consume para inicializar el stock en 0.
 *
 * Campos:
 *  - productId   → clave de partición Kafka + FK lógica en inventario
 *  - name, price → datos denormalizados (evita llamada HTTP al crear inventario)
 *  - minStock     → umbral de alerta, configurable, por defecto 0
 */
@Getter
public class ProductCreatedEvent implements DomainEvent {

    @JsonProperty("eventId")
    private final String eventId = UUID.randomUUID().toString();

    @JsonProperty("occurredAt")
    private final Instant occurredAt = Instant.now();

    @JsonProperty("productId")
    private final String productId;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("price")
    private final BigDecimal price;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("minStock")
    private final int minStock;

    public ProductCreatedEvent(String productId, String name, BigDecimal price,
                               String description, int minStock) {
        this.productId   = productId;
        this.name        = name;
        this.price       = price;
        this.description = description;
        this.minStock    = minStock;
    }
}
