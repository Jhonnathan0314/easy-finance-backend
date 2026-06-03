package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record AnnualBudgetImportRowResponseDto(
        int rowNumber,
        boolean valid,
        List<Integer> appliedMonths,
        List<String> errors
) {
}

