package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record PaymentMethodImportResponseDto(
        int createdCount,
        List<PaymentMethodImportRowResponseDto> rows
) {
}

