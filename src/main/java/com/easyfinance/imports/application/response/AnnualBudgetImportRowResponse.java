package com.easyfinance.imports.application.response;

import java.util.List;

public record AnnualBudgetImportRowResponse(
        int rowNumber,
        boolean valid,
        List<Integer> appliedMonths,
        List<String> errors
) {
}

