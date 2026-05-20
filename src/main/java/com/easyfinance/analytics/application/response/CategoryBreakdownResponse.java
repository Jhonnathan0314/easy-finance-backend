package com.easyfinance.analytics.application.response;

import java.time.LocalDate;
import java.util.List;

public record CategoryBreakdownResponse(
        Long accountId,
        LocalDate from,
        LocalDate to,
        List<CategoryAmountItem> items
) {
}
