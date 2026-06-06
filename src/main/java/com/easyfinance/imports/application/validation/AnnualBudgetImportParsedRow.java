package com.easyfinance.imports.application.validation;

import java.math.BigDecimal;
import java.util.List;

public record AnnualBudgetImportParsedRow(
        int rowNumber,
        Integer year,
        AnnualBudgetImportMonthScope monthScope,
        String budgetName,
        String categoryName,
        String subBudgetName,
        BigDecimal plannedAmount,
        String participantLabel,
        List<String> errors
) {
    public AnnualBudgetImportParsedRow(
            int rowNumber,
            Integer year,
            AnnualBudgetImportMonthScope monthScope,
            String budgetName,
            String categoryName,
            String subBudgetName,
            BigDecimal plannedAmount,
            List<String> errors
    ) {
        this(rowNumber, year, monthScope, budgetName, categoryName, subBudgetName, plannedAmount, null, errors);
    }
}
