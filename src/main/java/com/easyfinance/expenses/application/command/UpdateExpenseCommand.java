package com.easyfinance.expenses.application.command;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record UpdateExpenseCommand(
        Long accountId,
        Long expenseId,
        Long participantId,
        Long categoryId,
        Long paymentMethodId,
        String description,
        Money amount,
        LocalDate expenseDate,
        ExpensePaymentState paymentState
) {
}
