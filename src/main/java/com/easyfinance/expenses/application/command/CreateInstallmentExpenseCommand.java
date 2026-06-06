package com.easyfinance.expenses.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateInstallmentExpenseCommand(
        Long accountId,
        Long participantId,
        Long categoryId,
        Long paymentMethodId,
        String description,
        Money totalAmount,
        LocalDate expenseDate,
        Integer installmentCount,
        Money installmentAmount,
        LocalDate firstInstallmentDate,
        String debtName,
        String notes
) {
}
