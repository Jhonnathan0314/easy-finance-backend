package com.easyfinance.income.application.query;

import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record ListIncomesQuery(
        Long accountId,
        Integer year,
        Integer month,
        LocalDate from,
        LocalDate to,
        Long categoryId,
        Long participantId,
        IncomeStatus status,
        String search,
        PageQuery pageQuery,
        String sort
) {
    public ListIncomesQuery {
        if (month != null && year == null) {
            throw new BusinessRuleViolationException("INCOME_DATE_INVALID", "Income month requires a year.");
        }
        if (year != null && (year < 2000 || year > 2100)) {
            throw new BusinessRuleViolationException("INCOME_DATE_INVALID", "Income year is invalid.");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessRuleViolationException("INCOME_DATE_INVALID", "Income month is invalid.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("INCOME_DATE_INVALID", "Date range is invalid.");
        }
        search = normalizeSearch(search);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
