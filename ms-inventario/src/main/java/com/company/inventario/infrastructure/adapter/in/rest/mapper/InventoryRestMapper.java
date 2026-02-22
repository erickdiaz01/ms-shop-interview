package com.company.inventario.infrastructure.adapter.in.rest.mapper;
import com.company.inventario.application.query.*;
import com.company.inventario.infrastructure.adapter.in.rest.dto.*;
import org.springframework.stereotype.Component;

@Component
public class InventoryRestMapper {

    public InventoryResponse toInventoryResponse(InventoryResult r) {
        return new InventoryResponse(
            r.inventoryId(), "inventories",
            new InventoryResponse.InventoryAttributes(
                r.productId(), r.productName(), r.productDescription(),
                r.productPrice(), r.quantity(), r.minStock(), r.lowStock(), r.updatedAt()
            )
        );
    }

    public PurchaseResponse toPurchaseResponse(PurchaseResult r) {
        return new PurchaseResponse(
            r.purchaseId(), "purchases",
            new PurchaseResponse.PurchaseAttributes(
                r.productId(), r.productName(), r.quantity(),
                r.unitPrice(), r.totalAmount(), r.remainingStock(), r.purchasedAt()
            )
        );
    }
}
