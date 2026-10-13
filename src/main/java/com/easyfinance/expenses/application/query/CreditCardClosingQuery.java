package com.easyfinance.expenses.application.query;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingQuery(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        List<Long> categoryIds,
        ExpensePaymentState targetState
) {
    public CreditCardClosingQuery(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to) {
        this(accountId, paymentMethodId, from, to, null, null);
    }

    public CreditCardClosingQuery(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to, List<Long> categoryIds) {
        this(accountId, paymentMethodId, from, to, categoryIds, null);
    }

    public CreditCardClosingQuery {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Date range is invalid.");
        }
        targetState = targetState == null ? ExpensePaymentState.PAID : targetState;
        if (targetState != ExpensePaymentState.PAID && targetState != ExpensePaymentState.IN_POCKET) {
            throw new BusinessRuleViolationException("CREDIT_CARD_CLOSING_INVALID_TARGET_STATE", "Target state must be PAID or IN_POCKET.");
        }
        categoryIds = categoryIds == null || categoryIds.isEmpty() ? null : List.copyOf(categoryIds);
    }
}
