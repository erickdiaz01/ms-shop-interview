package com.company.inventario.infrastructure.adapter.in.rest.controller;

import com.company.inventario.domain.port.PurchaseRepository;
import com.company.inventario.infrastructure.adapter.in.rest.dto.JsonApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class PurchaseHistoryController {

    private final PurchaseRepository purchaseRepository;

    @GetMapping("/{productId}/history")
    @Operation(summary = "Get purchase history for a product")
    public Mono<JsonApiResponse<Object>> getHistory(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return purchaseRepository.findByProductId(UUID.fromString(productId), page, size)
                .map(p -> (Object) new PurchaseHistoryItem(
                        p.getId().toString(), "purchase_history",
                        new PurchaseHistoryItem.Attrs(
                                p.getProductId().toString(), p.getQuantity(),
                                p.getUnitPrice(), p.getTotalAmount(),
                                p.getCorrelationId(), p.getPurchasedAt())
                ))
                .collectList()
                .map(items -> JsonApiResponse.ofList(items,
                        new JsonApiResponse.Meta(items.size(), page, size, null)));
    }

    record PurchaseHistoryItem(String id, String type, Attrs attributes) {
        record Attrs(String productId, int quantity, BigDecimal unitPrice,
                     BigDecimal totalAmount, String correlationId, Instant purchasedAt) {}
    }
}
