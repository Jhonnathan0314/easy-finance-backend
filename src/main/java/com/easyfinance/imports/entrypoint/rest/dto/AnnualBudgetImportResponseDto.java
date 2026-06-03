package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record AnnualBudgetImportResponseDto(
        Integer year,
        int createdBudgetsCount,
        int createdSubBudgetsCount,
        List<AnnualBudgetImportRowResponseDto> rows
) {
}

