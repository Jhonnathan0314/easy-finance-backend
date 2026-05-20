package com.easyfinance.catalogs.application.command;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;

public record UpdatePaymentMethodCommand(
        Long accountId,
        Long paymentMethodId,
        String name,
        String description,
        PaymentMethodType type
) {
}
