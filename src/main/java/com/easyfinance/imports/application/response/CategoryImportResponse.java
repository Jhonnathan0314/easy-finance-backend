package com.easyfinance.imports.application.response;

import java.util.List;

public record CategoryImportResponse(
        int createdCount,
        List<CategoryImportRowResponse> rows
) {
}

