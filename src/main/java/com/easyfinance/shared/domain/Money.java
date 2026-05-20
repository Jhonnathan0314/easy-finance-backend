package com.easyfinance.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, CurrencyCode currency) {

    private static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        if (currency != CurrencyCode.COP) {
            throw new BusinessRuleViolationException("UNSUPPORTED_CURRENCY", "Only COP is supported in the MVP.");
        }

        amount = normalize(amount);
    }

    public static Money cop(BigDecimal amount) {
        return new Money(amount, CurrencyCode.COP);
    }

    public static Money zeroCop() {
        return cop(BigDecimal.ZERO);
    }

    public static Money positive(BigDecimal amount, CurrencyCode currency) {
        Money money = new Money(amount, currency);
        if (money.amount.signum() <= 0) {
            throw new BusinessRuleViolationException("MONEY_AMOUNT_NOT_POSITIVE", "Money amount must be greater than zero.");
        }
        return money;
    }

    public static Money nonNegative(BigDecimal amount, CurrencyCode currency) {
        Money money = new Money(amount, currency);
        if (money.amount.signum() < 0) {
            throw new BusinessRuleViolationException("MONEY_AMOUNT_NEGATIVE", "Money amount cannot be negative.");
        }
        return money;
    }

    public Money plus(Money other) {
        ensureSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        ensureSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void ensureSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (currency != other.currency) {
            throw new BusinessRuleViolationException("CURRENCY_MISMATCH", "Money operations require the same currency.");
        }
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value.signum() < 0) {
            throw new BusinessRuleViolationException("MONEY_AMOUNT_NEGATIVE", "Money amount cannot be negative.");
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}

