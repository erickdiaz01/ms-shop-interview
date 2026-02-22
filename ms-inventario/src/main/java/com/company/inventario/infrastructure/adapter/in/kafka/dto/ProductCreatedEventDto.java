package com.company.inventario.infrastructure.adapter.in.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO para deserializar el ProductCreatedEvent publicado por ms-productos.
 * Forma parte de la Anti-Corruption Layer: el dominio de inventario
 * nunca conoce los objetos internos de ms-productos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreatedEventDto(

    @JsonProperty("eventId")
    String eventId,

    @JsonProperty("productId")
    String productId,

    @JsonProperty("name")
    String name,

    @JsonProperty("price")
    BigDecimal price,

    @JsonProperty("description")
    String description,

    @JsonProperty("minStock")
    int minStock,

    @JsonProperty("occurredAt")
    Instant occurredAt
) {}
