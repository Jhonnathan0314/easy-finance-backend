package com.easyfinance.income.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncomeTest {

    @Test
    void createValidIncome() {
        Income income = Income.create(1L, 2L, 10L, "Salary", Money.cop(new BigDecimal("2500000")), LocalDate.of(2026, 5, 10));

        assertThat(income.status()).isEqualTo(IncomeStatus.ACTIVE);
        assertThat(income.amount().amount()).isEqualByComparingTo("2500000.00");
    }

    @Test
    void rejectZeroAmount() {
        assertThatThrownBy(() -> Income.create(1L, 2L, 10L, "Salary", Money.cop(BigDecimal.ZERO), LocalDate.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_AMOUNT_INVALID"));
    }

    @Test
    void cancelActiveIncome() {
        Income cancelled = income(IncomeStatus.ACTIVE).cancel();

        assertThat(cancelled.status()).isEqualTo(IncomeStatus.CANCELLED);
    }

    @Test
    void cancelledIncomeCannotBeUpdated() {
        Income cancelled = income(IncomeStatus.CANCELLED);

        assertThatThrownBy(() -> cancelled.update(2L, 10L, "Updated", Money.cop(new BigDecimal("100000")), LocalDate.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_ALREADY_CANCELLED"));
    }

    private static Income income(IncomeStatus status) {
        return Income.restore(
                5L,
                1L,
                2L,
                10L,
                "Salary",
                Money.cop(new BigDecimal("2500000")),
                LocalDate.of(2026, 5, 10),
                status,
                Instant.now(),
                Instant.now()
        );
    }
}
