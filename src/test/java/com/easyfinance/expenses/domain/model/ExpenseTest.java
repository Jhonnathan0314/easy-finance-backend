package com.easyfinance.expenses.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseTest {

    @Test
    void createSimpleExpenseDefaultsToPaidActiveAndSimple() {
        Expense expense = Expense.createSimple(1L, 2L, 3L, 4L, "Lunch", Money.cop(new BigDecimal("12000")), LocalDate.now(), null);

        assertThat(expense.paymentState()).isEqualTo(ExpensePaymentState.PAID);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(expense.expenseType()).isEqualTo(ExpenseType.SIMPLE);
    }

    @Test
    void createInstallmentExpenseDefaultsToPendingActiveAndInstallment() {
        Expense expense = Expense.createInstallment(1L, 2L, 3L, 4L, "Laptop", Money.cop(new BigDecimal("1200000")), LocalDate.now());

        assertThat(expense.paymentState()).isEqualTo(ExpensePaymentState.PENDING);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(expense.expenseType()).isEqualTo(ExpenseType.INSTALLMENT);
    }

    @Test
    void rejectInvalidAmount() {
        assertThatThrownBy(() -> Expense.createSimple(1L, 2L, 3L, 4L, "Lunch", Money.zeroCop(), LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_AMOUNT_INVALID"));
    }

    @Test
    void cancelledExpenseCannotBeUpdated() {
        Expense expense = Expense.createSimple(1L, 2L, 3L, 4L, "Lunch", Money.cop(new BigDecimal("12000")), LocalDate.now(), null)
                .cancel();

        assertThatThrownBy(() -> expense.update(2L, 3L, "Dinner", Money.cop(new BigDecimal("15000")), LocalDate.now(), ExpensePaymentState.PAID))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_ALREADY_CANCELLED"));
    }
}
