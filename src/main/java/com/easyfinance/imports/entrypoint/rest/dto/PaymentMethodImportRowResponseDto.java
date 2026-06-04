package com.easyfinance.imports.entrypoint.rest.dto;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;

import java.util.List;

public record PaymentMethodImportRowResponseDto(
        Integer rowNumber,
        String name,
        String description,
        PaymentMethodType type,
        boolean valid,
        Long createdPaymentMethodId,
        List<String> errors
) {
}
