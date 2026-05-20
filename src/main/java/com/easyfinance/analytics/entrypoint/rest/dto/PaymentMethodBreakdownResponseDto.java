package com.easyfinance.analytics.entrypoint.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record PaymentMethodBreakdownResponseDto(
        Long accountId,
        LocalDate from,
        LocalDate to,
        List<PaymentMethodAmountItemDto> items
) {
}
