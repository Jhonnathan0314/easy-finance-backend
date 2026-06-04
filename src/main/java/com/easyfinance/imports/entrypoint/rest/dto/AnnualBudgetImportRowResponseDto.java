package com.easyfinance.imports.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnnualBudgetImportRowResponseDto(
        int rowNumber,
        Integer year,
        String month,
        String budgetName,
        String categoryName,
        Long categoryId,
        String subBudgetName,
        BigDecimal plannedAmount,
        boolean valid,
        List<Integer> appliedMonths,
        List<String> errors
) {
}
