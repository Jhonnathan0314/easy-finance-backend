package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record PaymentMethodAmountItemDto(
        Long paymentMethodId,
        String paymentMethodName,
        BigDecimal amount,
        Long count
) {
}
