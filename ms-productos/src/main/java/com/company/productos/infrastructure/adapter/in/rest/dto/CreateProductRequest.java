package com.company.productos.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 200) String name,

    @NotNull @DecimalMin("0.00") BigDecimal price,

    String description
) {}
