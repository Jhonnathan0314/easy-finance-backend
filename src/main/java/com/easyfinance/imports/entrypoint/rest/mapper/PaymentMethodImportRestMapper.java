package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.PaymentMethodImportResponse;
import com.easyfinance.imports.entrypoint.rest.dto.PaymentMethodImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.PaymentMethodImportRowResponseDto;

public final class PaymentMethodImportRestMapper {

    private PaymentMethodImportRestMapper() {
    }

    public static PaymentMethodImportResponseDto toDto(PaymentMethodImportResponse response) {
        return new PaymentMethodImportResponseDto(
                response.createdCount(),
                response.rows().stream()
                        .map(row -> new PaymentMethodImportRowResponseDto(row.rowNumber(), row.valid(), row.createdPaymentMethodId(), row.errors()))
                        .toList()
        );
    }
}

