package com.easyfinance.budgets.domain.model;

import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubBudgetTest {

    @Test
    void createsManualSubBudget() {
        SubBudget subBudget = SubBudget.createManual(1L, 2L, 3L, "Food", Money.cop(new BigDecimal("100000")));

        assertThat(subBudget.sourceType()).isEqualTo(SubBudgetSourceType.MANUAL);
        assertThat(subBudget.debtId()).isNull();
        assertThat(subBudget.spentAmount().amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void debtDerivedCannotBeEditedManually() {
        SubBudget subBudget = SubBudget.createDebtDerived(1L, 2L, 3L, 4L, "Debt", Money.cop(new BigDecimal("100000")));

        assertThatThrownBy(() -> subBudget.updateManual(3L, "Updated", Money.cop(new BigDecimal("100000"))))
                .hasMessage("Only manual sub-budgets can be edited from this endpoint.");
    }

    @Test
    void manualSubBudgetCannotHaveDebtId() {
        assertThatThrownBy(() -> SubBudget.restore(1L, 1L, 2L, 3L, 4L, "Manual", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, null, null))
                .hasMessage("Manual sub-budgets cannot be linked to a debt.");
    }

    @Test
    void debtDerivedRequiresDebtId() {
        assertThatThrownBy(() -> SubBudget.createDebtDerived(1L, 2L, 3L, null, "Debt", Money.cop(new BigDecimal("100000"))))
                .hasMessage("Debt-derived sub-budget requires a debt id.");
    }
}
