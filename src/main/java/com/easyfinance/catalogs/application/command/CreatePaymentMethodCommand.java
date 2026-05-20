package com.easyfinance.catalogs.application.command;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;

public record CreatePaymentMethodCommand(
        Long accountId,
        String name,
        String description,
        PaymentMethodType type
) {
}
