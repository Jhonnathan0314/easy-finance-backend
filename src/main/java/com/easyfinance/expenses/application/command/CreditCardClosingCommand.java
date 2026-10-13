package com.easyfinance.expenses.application.command;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;

import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingCommand(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        List<Long> categoryIds,
        ExpensePaymentState targetState,
        List<Long> expenseIds
) {
    public CreditCardClosingCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to) {
        this(accountId, paymentMethodId, from, to, null, null, null);
    }

    public CreditCardClosingCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to, List<Long> categoryIds) {
        this(accountId, paymentMethodId, from, to, categoryIds, null, null);
    }

    public CreditCardClosingCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to, List<Long> categoryIds, ExpensePaymentState targetState) {
        this(accountId, paymentMethodId, from, to, categoryIds, targetState, null);
    }

    public CreditCardClosingCommand {
        targetState = targetState == null ? ExpensePaymentState.PAID : targetState;
    }
}
