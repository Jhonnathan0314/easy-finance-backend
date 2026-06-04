package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record PaymentMethodTypeAmountItemDto(
        String paymentMethodType,
        BigDecimal amount,
        Long count
) {
}
