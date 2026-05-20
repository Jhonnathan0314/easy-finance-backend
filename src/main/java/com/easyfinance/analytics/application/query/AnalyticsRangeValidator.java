package com.easyfinance.analytics.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

final class AnalyticsRangeValidator {

    private static final int MAX_MONTHS = 24;

    private AnalyticsRangeValidator() {
    }

    static void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("ANALYTICS_DATE_RANGE_INVALID", "Analytics date range is invalid.");
        }
        if (to.isAfter(from.plusMonths(MAX_MONTHS))) {
            throw new BusinessRuleViolationException("ANALYTICS_DATE_RANGE_TOO_LARGE", "Analytics date range cannot be greater than 24 months.");
        }
    }
}
