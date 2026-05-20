package com.easyfinance.analytics.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record CashflowQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        CashflowGroupBy groupBy,
        Long participantId
) {
    public CashflowQuery {
        AnalyticsRangeValidator.validate(from, to);
        if (groupBy == null) {
            throw new BusinessRuleViolationException("ANALYTICS_GROUP_BY_INVALID", "Analytics groupBy is invalid.");
        }
    }
}
