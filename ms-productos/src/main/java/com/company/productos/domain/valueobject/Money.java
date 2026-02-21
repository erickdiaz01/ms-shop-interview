package com.company.productos.domain.valueobject;
import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) {
    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
    }
    public static Money of(BigDecimal v) { return new Money(v); }
    public static Money of(double v)     { return new Money(BigDecimal.valueOf(v)); }
    public static Money zero()           { return new Money(BigDecimal.ZERO); }
}
