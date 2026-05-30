package com.easyfinance.imports.application.response;

import java.util.List;

public record IncomeImportRowResponse(
        Integer rowNumber,
        boolean valid,
        Long createdIncomeId,
        List<String> errors
) {
}

