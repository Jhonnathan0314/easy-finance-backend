package com.easyfinance.expenses.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record CreditCardClosingQuery(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to
) {
    public CreditCardClosingQuery {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Date range is invalid.");
        }
    }
}
