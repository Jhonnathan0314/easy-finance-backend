package com.easyfinance.imports.domain.model;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.shared.domain.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseImportRow(
        Long id,
        Long accountId,
        Long batchId,
        Integer rowNumber,
        LocalDate expenseDate,
        String description,
        Money amount,
        String categoryName,
        Long categoryId,
        String paymentMethodName,
        Long paymentMethodId,
        ExpensePaymentState paymentState,
        String participantLabel,
        Long participantId,
        boolean appliesDebtPayment,
        Long debtId,
        String debtLabel,
        DebtPaymentType debtPaymentType,
        String debtPaymentNotes,
        boolean valid,
        List<ImportRowError> errors,
        Long createdExpenseId,
        Long createdDebtPaymentId,
        Instant createdAt,
        Instant updatedAt
) {
    public ExpenseImportRow {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ExpenseImportRow(
            Long id,
            Long accountId,
            Long batchId,
            Integer rowNumber,
            LocalDate expenseDate,
            String description,
            Money amount,
            String categoryName,
            Long categoryId,
            String paymentMethodName,
            Long paymentMethodId,
            ExpensePaymentState paymentState,
            boolean appliesDebtPayment,
            Long debtId,
            String debtLabel,
            DebtPaymentType debtPaymentType,
            String debtPaymentNotes,
            boolean valid,
            List<ImportRowError> errors,
            Long createdExpenseId,
            Long createdDebtPaymentId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, accountId, batchId, rowNumber, expenseDate, description, amount, categoryName, categoryId, paymentMethodName, paymentMethodId, paymentState, null, null, appliesDebtPayment, debtId, debtLabel, debtPaymentType, debtPaymentNotes, valid, errors, createdExpenseId, createdDebtPaymentId, createdAt, updatedAt);
    }
}
