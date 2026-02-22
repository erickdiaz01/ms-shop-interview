package com.company.inventario.infrastructure.adapter.out.persistence.entity;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("purchase_history")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseEntity {
    @Id private UUID id;
    private UUID productId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String correlationId;
    @CreatedDate private Instant purchasedAt;
}
