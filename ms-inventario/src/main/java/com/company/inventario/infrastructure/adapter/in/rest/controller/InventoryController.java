package com.company.inventario.infrastructure.adapter.in.rest.controller;

import com.company.inventario.application.command.*;
import com.company.inventario.application.handler.*;
import com.company.inventario.infrastructure.adapter.in.rest.dto.*;
import com.company.inventario.infrastructure.adapter.in.rest.mapper.InventoryRestMapper;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "Inventory management and purchase API")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final GetInventoryQueryHandler getInventoryHandler;
    private final UpdateStockCommandHandler updateStockHandler;
    private final PurchaseCommandHandler purchaseHandler;
    private final InventoryRestMapper mapper;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory for a product (includes product info from ms-productos)")
    public Mono<JsonApiResponse<InventoryResponse>> getInventory(@PathVariable String productId) {
        log.info("GET /api/v1/inventory/{}", productId);
        return getInventoryHandler.handle(new GetInventoryCommand(productId))
                .map(r -> JsonApiResponse.of(mapper.toInventoryResponse(r)));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update stock quantity for a product")
    public Mono<JsonApiResponse<InventoryResponse>> updateStock(
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        log.info("PATCH /api/v1/inventory/{} -> qty={}", productId, request.quantity());
        return updateStockHandler.handle(new UpdateStockCommand(productId, request.quantity(), request.correlationId()))
                .map(r -> JsonApiResponse.of(mapper.toInventoryResponse(r)));
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Process a purchase — verifies stock, discounts, emits event")
    public Mono<JsonApiResponse<PurchaseResponse>> purchase(
            @Valid @RequestBody PurchaseRequest request) {
        log.info("POST /api/v1/inventory/purchase - productId={}, qty={}",
                request.productId(), request.quantity());
        return purchaseHandler.handle(
                new PurchaseCommand(request.productId(), request.quantity(), request.correlationId()))
                .map(r -> JsonApiResponse.of(mapper.toPurchaseResponse(r)));
    }
}
