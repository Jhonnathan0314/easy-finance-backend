package com.easyfinance.expenses.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingQuery(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        List<Long> categoryIds
) {
    public CreditCardClosingQuery(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to) {
        this(accountId, paymentMethodId, from, to, null);
    }

    public CreditCardClosingQuery {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Date range is invalid.");
        }
        categoryIds = categoryIds == null || categoryIds.isEmpty() ? null : List.copyOf(categoryIds);
    }
}
