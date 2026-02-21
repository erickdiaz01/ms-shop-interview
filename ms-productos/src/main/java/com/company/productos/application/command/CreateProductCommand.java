package com.company.productos.application.command;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductCommand(
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price must be >= 0")
    BigDecimal price,

    String description
) {}
