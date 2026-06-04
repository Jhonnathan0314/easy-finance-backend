package com.easyfinance.imports.application.response;

import java.math.BigDecimal;
import java.util.List;

public record AnnualBudgetImportRowResponse(
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
