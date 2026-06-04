package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record PaymentMethodTypeAmountItem(
        String paymentMethodType,
        BigDecimal amount,
        Long count
) {
}
