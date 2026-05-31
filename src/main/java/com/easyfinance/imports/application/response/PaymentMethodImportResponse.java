package com.easyfinance.imports.application.response;

import java.util.List;

public record PaymentMethodImportResponse(
        int createdCount,
        List<PaymentMethodImportRowResponse> rows
) {
}

