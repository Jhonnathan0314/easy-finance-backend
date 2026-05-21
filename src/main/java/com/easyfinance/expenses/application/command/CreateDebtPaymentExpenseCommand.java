package com.easyfinance.expenses.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateDebtPaymentExpenseCommand(
        Long accountId,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        Long debtPaymentId,
        String description,
        Money amount,
        LocalDate expenseDate
) {
}
