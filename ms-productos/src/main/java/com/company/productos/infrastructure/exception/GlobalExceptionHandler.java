package com.company.productos.infrastructure.exception;

import com.company.productos.domain.exception.DuplicateProductException;
import com.company.productos.domain.exception.ProductNotFoundException;
import com.company.productos.infrastructure.adapter.in.rest.dto.JsonApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<JsonApiResponse<?>> handleNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("404", "PRODUCT_NOT_FOUND", "Product not found", ex.getMessage())
        )));
    }

    @ExceptionHandler(DuplicateProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<JsonApiResponse<?>> handleDuplicate(DuplicateProductException ex) {
        log.warn("Duplicate product: {}", ex.getMessage());
        return Mono.just(JsonApiResponse.error(List.of(
                new JsonApiResponse.ApiError("409", "DUPLICATE_PRODUCT", "Product already exists", ex.getMessage())
        )));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<JsonApiResponse<?>> handleValidation(WebExchangeBindException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new JsonApiResponse.ApiError(
                        "400", "VALIDATION_ERROR",
                        "Validation failed for field: " + fe.getField(),
                        fe.getDefaultMessage()))
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
                new JsonApiResponse.ApiError("500", "INTERNAL_ERROR", "Unexpected error", "An unexpected error occurred")
        )));
    }
}
