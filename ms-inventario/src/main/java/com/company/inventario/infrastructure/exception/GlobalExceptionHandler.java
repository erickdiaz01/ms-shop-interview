package com.company.inventario.infrastructure.exception;

import com.company.inventario.domain.exception.*;
import com.company.inventario.infrastructure.adapter.in.rest.dto.JsonApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<JsonApiResponse<?>> handleInventoryNotFound(InventoryNotFoundException ex) {
        log.warn("Inventory not found: {}", ex.getMessage());
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("404", "INVENTORY_NOT_FOUND", "Inventory not found", ex.getMessage())
        )));
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Mono<JsonApiResponse<?>> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("422", "INSUFFICIENT_STOCK",
                        "Insufficient stock", ex.getMessage())
        )));
    }

    @ExceptionHandler(ProductServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Mono<JsonApiResponse<?>> handleProductServiceError(ProductServiceException ex) {
        log.error("Product service error: {}", ex.getMessage());
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("502", "PRODUCT_SERVICE_ERROR",
                        "Product service unavailable", ex.getMessage())
        )));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<JsonApiResponse<?>> handleValidation(WebExchangeBindException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new JsonApiResponse.ApiError("400", "VALIDATION_ERROR",
                        "Validation failed: " + fe.getField(), fe.getDefaultMessage()))
                .toList();
        return Mono.just(JsonApiResponse.error(errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<JsonApiResponse<?>> handleIllegalArg(IllegalArgumentException ex) {
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("400", "BAD_REQUEST", "Invalid argument", ex.getMessage())
        )));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<JsonApiResponse<?>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("500", "INTERNAL_ERROR", "Unexpected error",
                        "An unexpected error occurred")
        )));
    }
}
