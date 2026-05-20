package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record PaymentMethodAmountItem(
        Long paymentMethodId,
        String paymentMethodName,
        BigDecimal amount,
        Long count
) {
}
