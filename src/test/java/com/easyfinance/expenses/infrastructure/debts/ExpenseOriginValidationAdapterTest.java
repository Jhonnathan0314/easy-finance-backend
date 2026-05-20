package com.easyfinance.expenses.infrastructure.debts;

import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpenseOriginValidationAdapterTest {

    private final ExpenseRepositoryPort expenseRepository = mock(ExpenseRepositoryPort.class);
    private final ExpenseOriginValidationAdapter adapter = new ExpenseOriginValidationAdapter(expenseRepository);

    @Test
    void installmentOriginWorks() {
        when(expenseRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.of(expense(ExpenseType.INSTALLMENT)));

        assertThatCode(() -> adapter.validateInstallmentOrigin(1L, 99L))
                .doesNotThrowAnyException();
    }

    @Test
    void simpleOriginFails() {
        when(expenseRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.of(expense(ExpenseType.SIMPLE)));

        assertThatThrownBy(() -> adapter.validateInstallmentOrigin(1L, 99L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> org.assertj.core.api.Assertions.assertThat(ex.code()).isEqualTo("DEBT_ORIGIN_EXPENSE_INVALID_TYPE"));
    }

    @Test
    void originFromAnotherAccountFailsAsNotFound() {
        when(expenseRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.validateInstallmentOrigin(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> org.assertj.core.api.Assertions.assertThat(ex.code()).isEqualTo("DEBT_ORIGIN_EXPENSE_NOT_FOUND"));
    }

    private static Expense expense(ExpenseType type) {
        return Expense.restore(
                99L,
                1L,
                2L,
                3L,
                10L,
                "Laptop",
                Money.cop(new BigDecimal("1200000")),
                LocalDate.of(2026, 5, 11),
                ExpensePaymentState.PENDING,
                ExpenseStatus.ACTIVE,
                type,
                Instant.now(),
                Instant.now()
        );
    }
}
