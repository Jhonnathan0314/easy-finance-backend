package com.easyfinance.analytics.application.query;

import java.time.LocalDate;

public record CashflowSummaryQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        Long participantId,
        Long categoryId,
        Long paymentMethodId
) {
    public CashflowSummaryQuery {
        AnalyticsRangeValidator.validate(from, to);
    }
}
