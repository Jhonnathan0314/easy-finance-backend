package com.easyfinance.imports.application.response;

import java.util.List;

public record AnnualBudgetImportResponse(
        Integer year,
        int createdBudgetsCount,
        int createdSubBudgetsCount,
        List<AnnualBudgetImportRowResponse> rows
) {
}

