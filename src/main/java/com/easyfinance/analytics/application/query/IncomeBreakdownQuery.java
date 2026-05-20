package com.easyfinance.analytics.application.query;

import com.easyfinance.income.domain.model.IncomeStatus;

import java.time.LocalDate;

public record IncomeBreakdownQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        Long categoryId,
        Long participantId,
        IncomeStatus status
) {
    public IncomeBreakdownQuery {
        AnalyticsRangeValidator.validate(from, to);
    }
}
