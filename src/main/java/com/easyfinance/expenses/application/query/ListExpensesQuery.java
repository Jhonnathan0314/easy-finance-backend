package com.easyfinance.expenses.application.query;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;
import java.util.List;

public record ListExpensesQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        List<Long> categoryIds,
        List<Long> paymentMethodIds,
        Long participantId,
        ExpensePaymentState paymentState,
        ExpenseStatus status,
        ExpenseType expenseType,
        String search,
        Boolean debtPaymentOrigin,
        PageQuery pageQuery,
        String sort
) {
    public ListExpensesQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("EXPENSE_DATE_INVALID", "Date range is invalid.");
        }
        search = normalizeSearch(search);
        categoryIds = normalizeIds(categoryIds);
        paymentMethodIds = normalizeIds(paymentMethodIds);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<Long> normalizeIds(List<Long> values) {
        return values == null || values.isEmpty() ? null : values;
    }
}
