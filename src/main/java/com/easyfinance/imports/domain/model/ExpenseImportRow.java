package com.easyfinance.imports.domain.model;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
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
        boolean valid,
        List<ImportRowError> errors,
        Long createdExpenseId,
        Instant createdAt,
        Instant updatedAt
) {
    public ExpenseImportRow {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
