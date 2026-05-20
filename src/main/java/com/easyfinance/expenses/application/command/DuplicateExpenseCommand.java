package com.easyfinance.expenses.application.command;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record DuplicateExpenseCommand(
        Long accountId,
        Long expenseId,
        LocalDate expenseDate,
        Money amount,
        String description,
        ExpensePaymentState paymentState
) {
}
