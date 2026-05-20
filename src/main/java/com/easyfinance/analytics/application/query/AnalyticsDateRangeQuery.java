package com.easyfinance.analytics.application.query;

import java.time.LocalDate;

public record AnalyticsDateRangeQuery(Long accountId, LocalDate from, LocalDate to) {
    public AnalyticsDateRangeQuery {
        AnalyticsRangeValidator.validate(from, to);
    }
}
