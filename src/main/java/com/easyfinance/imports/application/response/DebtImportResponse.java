package com.easyfinance.imports.application.response;

import java.util.List;

public record DebtImportResponse(
        int createdCount,
        List<DebtImportRowResponse> rows
) {
}
