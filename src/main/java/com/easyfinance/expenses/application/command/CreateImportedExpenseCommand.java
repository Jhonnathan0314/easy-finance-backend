package com.easyfinance.expenses.application.command;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateImportedExpenseCommand(
        Long accountId,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        String description,
        Money amount,
        LocalDate expenseDate,
        ExpensePaymentState paymentState
) {
}
