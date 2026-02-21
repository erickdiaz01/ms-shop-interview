package com.company.productos.infrastructure.adapter.in.rest.controller;

import com.company.productos.application.command.*;
import com.company.productos.application.handler.*;
import com.company.productos.infrastructure.adapter.in.rest.dto.*;
import com.company.productos.infrastructure.adapter.in.rest.mapper.ProductRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management API")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final CreateProductCommandHandler createHandler;
    private final GetProductQueryHandler getHandler;
    private final ListProductsQueryHandler listHandler;
    private final ProductRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new product")
    public Mono<JsonApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        log.info("POST /api/v1/products - name: {}", request.name());
        return createHandler.handle(new CreateProductCommand(request.name(), request.price(), request.description()))
                .map(result -> JsonApiResponse.of(mapper.toResponse(result)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID")
    public Mono<JsonApiResponse<ProductResponse>> getProduct(@PathVariable String id) {
        log.info("GET /api/v1/products/{}", id);
        return getHandler.handle(new GetProductCommand(id))
                .map(result -> JsonApiResponse.of(mapper.toResponse(result)));
    }

    @GetMapping
    @Operation(summary = "List all active products")
    public Mono<JsonApiResponse<ProductResponse>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return listHandler.handle(new ListProductsCommand(page, size))
                .map(paged -> JsonApiResponse.ofList(
                        paged.items().stream().map(mapper::toResponse).toList(),
                        new JsonApiResponse.Meta(paged.total(), paged.page(), paged.size(), null)
                ));
    }
}
