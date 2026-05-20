package com.easyfinance.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void createsCopMoneyWithTwoDecimalScale() {
        Money money = Money.cop(new BigDecimal("100.555"));

        assertThat(money.amount()).isEqualByComparingTo("100.56");
        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.currency()).isEqualTo(CurrencyCode.COP);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> Money.cop(new BigDecimal("-1.00")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Money amount cannot be negative.");
    }

    @Test
    void requiresPositiveAmountWhenRequested() {
        assertThatThrownBy(() -> Money.positive(BigDecimal.ZERO, CurrencyCode.COP))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Money amount must be greater than zero.");
    }

    @Test
    void rejectsBlankCurrencyCode() {
        assertThatThrownBy(() -> CurrencyCode.from(" "))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Currency is required.");
    }

    @Test
    void rejectsUnsupportedCurrencyCode() {
        assertThatThrownBy(() -> CurrencyCode.from("USD"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Only COP is supported in the MVP.");
    }
}

