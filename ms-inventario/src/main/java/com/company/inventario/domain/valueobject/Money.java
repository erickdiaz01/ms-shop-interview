package com.company.inventario.domain.valueobject;
import java.math.BigDecimal;
import java.util.Objects;
public record Money(BigDecimal amount) {
    public Money { Objects.requireNonNull(amount); }
    public static Money of(BigDecimal v) { return new Money(v); }
    public static Money of(double v)     { return new Money(BigDecimal.valueOf(v)); }
    public Money multiply(int qty)       { return new Money(amount.multiply(BigDecimal.valueOf(qty))); }
}
