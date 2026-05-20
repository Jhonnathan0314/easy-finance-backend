package com.easyfinance.budgets.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTest {

    @Test
    void createsValidBudget() {
        Budget budget = Budget.create(1L, 2026, 5, "May");

        assertThat(budget.status()).isEqualTo(BudgetStatus.ACTIVE);
        assertThat(budget.name()).isEqualTo("May");
    }

    @Test
    void invalidMonthFails() {
        assertThatThrownBy(() -> Budget.create(1L, 2026, 13, "Bad"))
                .hasMessage("Budget month is invalid.");
    }
}
