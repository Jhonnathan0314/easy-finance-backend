package com.easyfinance.imports.application.response;

import java.util.List;

public record IncomeImportResponse(
        int createdCount,
        List<IncomeImportRowResponse> rows
) {
}

