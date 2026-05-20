package com.easyfinance.budgets.domain.model;

import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetImpactTest {

    @Test
    void partialPaymentKeepsImpactActive() {
        BudgetImpact impact = impact().applyPayment(Money.cop(new BigDecimal("40000")));

        assertThat(impact.paidAmount().amount()).isEqualByComparingTo("40000.00");
        assertThat(impact.status()).isEqualTo(BudgetImpactStatus.ACTIVE);
    }

    @Test
    void fullPaymentMarksImpactPaid() {
        BudgetImpact impact = impact().applyPayment(Money.cop(new BigDecimal("100000")));

        assertThat(impact.paidAmount().amount()).isEqualByComparingTo("100000.00");
        assertThat(impact.status()).isEqualTo(BudgetImpactStatus.PAID);
    }

    @Test
    void paidAmountCannotExceedExpectedAmount() {
        assertThatThrownBy(() -> BudgetImpact.restore(1L, 1L, 2L, 3L, 4L, 5L, 2026, 5, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100001")), BudgetImpactStatus.ACTIVE, BudgetImpactSourceType.DEBT_INSTALLMENT, null, null))
                .hasMessage("Budget impact paid amount is invalid.");
    }

    private static BudgetImpact impact() {
        return BudgetImpact.createDebtInstallment(1L, 2L, 3L, 4L, 5L, 2026, 5, Money.cop(new BigDecimal("100000")));
    }
}
