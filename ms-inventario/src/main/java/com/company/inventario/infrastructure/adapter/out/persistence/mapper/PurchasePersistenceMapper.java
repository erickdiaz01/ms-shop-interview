package com.company.inventario.infrastructure.adapter.out.persistence.mapper;
import com.company.inventario.domain.model.Purchase;
import com.company.inventario.infrastructure.adapter.out.persistence.entity.PurchaseEntity;
import org.springframework.stereotype.Component;

@Component
public class PurchasePersistenceMapper {
    public PurchaseEntity toEntity(Purchase p) {
        return PurchaseEntity.builder()
            .id(p.getId()).productId(p.getProductId())
            .quantity(p.getQuantity()).unitPrice(p.getUnitPrice())
            .totalAmount(p.getTotalAmount()).correlationId(p.getCorrelationId())
            .purchasedAt(p.getPurchasedAt()).build();
    }
    public Purchase toDomain(PurchaseEntity e) {
        return Purchase.reconstitute(
            e.getId(), e.getProductId(), e.getQuantity(),
            e.getUnitPrice(), e.getTotalAmount(), e.getCorrelationId(), e.getPurchasedAt());
    }
}
