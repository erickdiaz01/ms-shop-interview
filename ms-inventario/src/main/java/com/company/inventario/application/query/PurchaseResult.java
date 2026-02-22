package com.company.inventario.application.query;
import java.math.BigDecimal;
import java.time.Instant;
public record PurchaseResult(
    String purchaseId,
    String productId,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    int remainingStock,
    Instant purchasedAt
) {}
