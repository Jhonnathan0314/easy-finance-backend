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
        assertThat(expense.sourceType()).isEqualTo(ExpenseSourceType.MANUAL);
        assertThat(expense.sourceDebtPaymentId()).isNull();
    }

    @Test
    void createInstallmentExpenseDefaultsToPendingActiveAndInstallment() {
        Expense expense = Expense.createInstallment(1L, 2L, 3L, 4L, "Laptop", Money.cop(new BigDecimal("1200000")), LocalDate.now());

        assertThat(expense.paymentState()).isEqualTo(ExpensePaymentState.PENDING);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(expense.expenseType()).isEqualTo(ExpenseType.INSTALLMENT);
        assertThat(expense.sourceType()).isEqualTo(ExpenseSourceType.MANUAL);
    }

    @Test
    void createImportedExpenseMarksSourceImport() {
        Expense expense = Expense.createImported(1L, 2L, 3L, 4L, "Lunch", Money.cop(new BigDecimal("12000")), LocalDate.now(), ExpensePaymentState.PAID);

        assertThat(expense.sourceType()).isEqualTo(ExpenseSourceType.IMPORT);
        assertThat(expense.sourceDebtPaymentId()).isNull();
    }

    @Test
    void createDebtPaymentExpenseRequiresAndStoresDebtPaymentId() {
        Expense expense = Expense.createDebtPayment(1L, 2L, 3L, 4L, 30L, 50L, "Debt payment", Money.cop(new BigDecimal("12000")), LocalDate.now());

        assertThat(expense.paymentState()).isEqualTo(ExpensePaymentState.PAID);
        assertThat(expense.expenseType()).isEqualTo(ExpenseType.SIMPLE);
        assertThat(expense.sourceType()).isEqualTo(ExpenseSourceType.DEBT_PAYMENT);
        assertThat(expense.sourceDebtPaymentId()).isEqualTo(50L);
        assertThat(expense.sourceDebtId()).isEqualTo(30L);
    }

    @Test
    void debtPaymentExpenseRejectsMissingDebtPaymentId() {
        assertThatThrownBy(() -> Expense.createDebtPayment(1L, 2L, 3L, 4L, 30L, null, "Debt payment", Money.cop(new BigDecimal("12000")), LocalDate.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_SOURCE_DEBT_PAYMENT_REQUIRED"));
    }

    @Test
    void debtPaymentExpenseRejectsMissingDebtId() {
        assertThatThrownBy(() -> Expense.createDebtPayment(1L, 2L, 3L, 4L, null, 50L, "Debt payment", Money.cop(new BigDecimal("12000")), LocalDate.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_SOURCE_DEBT_REQUIRED"));
    }

    @Test
    void nonDebtPaymentExpenseCannotReferenceDebtPayment() {
        assertThatThrownBy(() -> Expense.restore(
                1L,
                1L,
                2L,
                3L,
                4L,
                "Lunch",
                Money.cop(new BigDecimal("12000")),
                LocalDate.now(),
                ExpensePaymentState.PAID,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.IMPORT,
                50L,
                null,
                null,
                null
        )).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_SOURCE_INVALID"));
    }

    @Test
    void nonDebtPaymentExpenseCannotReferenceDebt() {
        assertThatThrownBy(() -> Expense.restore(
                1L,
                1L,
                2L,
                3L,
                4L,
                "Lunch",
                Money.cop(new BigDecimal("12000")),
                LocalDate.now(),
                ExpensePaymentState.PAID,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.MANUAL,
                null,
                30L,
                null,
                null
        )).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_SOURCE_INVALID"));
    }

    @Test
    void legacyDebtPaymentExpenseWithoutSourceDebtIdRestoresSuccessfully() {
        Expense expense = Expense.restore(
                1L,
                1L,
                2L,
                3L,
                4L,
                "Debt payment",
                Money.cop(new BigDecimal("12000")),
                LocalDate.now(),
                ExpensePaymentState.PAID,
                ExpenseStatus.ACTIVE,
                ExpenseType.SIMPLE,
                ExpenseSourceType.DEBT_PAYMENT,
                50L,
                null,
                null,
                null
        );

        assertThat(expense.sourceDebtPaymentId()).isEqualTo(50L);
        assertThat(expense.sourceDebtId()).isNull();
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

        assertThatThrownBy(() -> expense.update(2L, 3L, 4L, "Dinner", Money.cop(new BigDecimal("15000")), LocalDate.now(), ExpensePaymentState.PAID))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_ALREADY_CANCELLED"));
    }
}
