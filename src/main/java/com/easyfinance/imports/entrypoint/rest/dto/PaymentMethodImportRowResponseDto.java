package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record PaymentMethodImportRowResponseDto(
        Integer rowNumber,
        boolean valid,
        Long createdPaymentMethodId,
        List<String> errors
) {
}

