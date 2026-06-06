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
        String participantLabel,
        Long participantId,
        boolean valid,
        List<Integer> appliedMonths,
        List<String> errors
) {
    public AnnualBudgetImportRowResponse(
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
        this(rowNumber, year, month, budgetName, categoryName, categoryId, subBudgetName, plannedAmount, null, null, valid, appliedMonths, errors);
    }
}
