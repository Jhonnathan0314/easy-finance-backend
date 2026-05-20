package com.easyfinance.analytics.application.response;

import com.easyfinance.analytics.application.query.CashflowGroupBy;

import java.time.LocalDate;
import java.util.List;

public record CashflowResponse(
        Long accountId,
        LocalDate from,
        LocalDate to,
        CashflowGroupBy groupBy,
        List<CashflowItem> items
) {
}
