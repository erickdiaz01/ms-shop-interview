package com.company.inventario.domain.valueobject;
public record Quantity(int value) {
    public Quantity {
        if (value < 0) throw new IllegalArgumentException("Quantity cannot be negative: " + value);
    }
    public static Quantity of(int v) { return new Quantity(v); }
    public static Quantity zero()    { return new Quantity(0); }
    public Quantity subtract(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount to subtract must be positive");
        return new Quantity(value - amount);
    }
    public boolean isSufficient(int needed) { return value >= needed; }
}
