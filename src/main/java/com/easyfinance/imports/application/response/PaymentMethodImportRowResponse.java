package com.easyfinance.imports.application.response;

import java.util.List;

public record PaymentMethodImportRowResponse(
        Integer rowNumber,
        boolean valid,
        Long createdPaymentMethodId,
        List<String> errors
) {
}

