package com.easyfinance.analytics.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

public record MonthlyAnalyticsQuery(Long accountId, Integer year, Integer month) {
    public MonthlyAnalyticsQuery {
        if (year == null || year < 2000 || year > 2100 || month == null || month < 1 || month > 12) {
            throw new BusinessRuleViolationException("ANALYTICS_PERIOD_INVALID", "Analytics period is invalid.");
        }
    }
}
