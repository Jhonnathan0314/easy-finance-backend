package com.easyfinance.expenses.application.query;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record ListExpensesQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        ExpensePaymentState paymentState,
        ExpenseStatus status,
        ExpenseType expenseType,
        String search,
        PageQuery pageQuery,
        String sort
) {
    public ListExpensesQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Date range is invalid.");
        }
        search = normalizeSearch(search);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
