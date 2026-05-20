package com.easyfinance.expenses.infrastructure.persistence;

import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseJpaEntity;
import com.easyfinance.expenses.infrastructure.persistence.jpa.SpringDataExpenseRepository;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaExpenseRepositoryAdapterTest {

    @Test
    void updateInAnotherAccountReturnsNotFound() {
        SpringDataExpenseRepository repository = mock(SpringDataExpenseRepository.class);
        JpaExpenseRepositoryAdapter adapter = new JpaExpenseRepositoryAdapter(repository);
        Expense expense = expense(99L, 2L);
        when(repository.findByAccountIdAndId(2L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(expense))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(any(ExpenseJpaEntity.class));
    }

    private static Expense expense(Long id, Long accountId) {
        return Expense.restore(
                id,
                accountId,
                2L,
                3L,
                4L,
                "Lunch",
                Money.cop(new BigDecimal("12000")),
                LocalDate.now(),
                ExpensePaymentState.PAID,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                Instant.now(),
                Instant.now()
        );
    }
}
