package com.easyfinance.analytics.application.response;

import java.time.LocalDate;
import java.util.List;

public record PaymentMethodTypeBreakdownResponse(
        Long accountId,
        LocalDate from,
        LocalDate to,
        List<PaymentMethodTypeAmountItem> items
) {
}
