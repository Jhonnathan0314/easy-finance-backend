package com.easyfinance.imports.application.response;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;

import java.util.List;

public record PaymentMethodImportRowResponse(
        Integer rowNumber,
        String name,
        String description,
        PaymentMethodType type,
        boolean valid,
        Long createdPaymentMethodId,
        List<String> errors
) {
}
