package com.company.inventario.domain;

import com.company.inventario.domain.event.InventoryUpdatedEvent;
import com.company.inventario.domain.exception.InsufficientStockException;
import com.company.inventario.domain.model.Inventory;
import com.company.inventario.domain.valueobject.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.create(ProductId.of("00000000-0000-0000-0000-000000000001"), 50, 5);
    }

    @Test
    void purchase_shouldReduceStock_whenSufficientQuantity() {
        inventory.purchase(10, "corr-1");
        assertThat(inventory.getQuantity().value()).isEqualTo(40);
    }

    @Test
    void purchase_shouldRaiseDomainEvent() {
        inventory.purchase(5, "corr-1");
        assertThat(inventory.getDomainEvents())
                .hasSize(1)
                .first().isInstanceOf(InventoryUpdatedEvent.class);
        var event = (InventoryUpdatedEvent) inventory.getDomainEvents().get(0);
        assertThat(event.getPreviousQuantity()).isEqualTo(50);
        assertThat(event.getNewQuantity()).isEqualTo(45);
        assertThat(event.getReason()).isEqualTo("PURCHASE");
    }

    @Test
    void purchase_shouldThrow_whenInsufficientStock() {
        assertThatThrownBy(() -> inventory.purchase(100, "corr-1"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 50, Requested: 100");
    }

    @Test
    void purchase_shouldThrow_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> inventory.purchase(0, "corr-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isLowStock_shouldReturnTrue_whenBelowMinStock() {
        inventory.purchase(47, "corr-1"); // leaves 3, minStock=5
        assertThat(inventory.isLowStock()).isTrue();
    }

    @Test
    void updateStock_shouldSetNewQuantity() {
        inventory.updateStock(200, "corr-1");
        assertThat(inventory.getQuantity().value()).isEqualTo(200);
    }

    @Test
    void quantity_shouldRejectNegativeValue() {
        assertThatThrownBy(() -> inventory.updateStock(-1, "corr-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
