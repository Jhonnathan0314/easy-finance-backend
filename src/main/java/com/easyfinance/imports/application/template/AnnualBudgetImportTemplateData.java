package com.easyfinance.imports.application.template;

import java.util.List;

public record AnnualBudgetImportTemplateData(
        List<String> expenseCategoryNames,
        List<String> participantLabels
) {
    public AnnualBudgetImportTemplateData(List<String> expenseCategoryNames) {
        this(expenseCategoryNames, List.of());
    }

    public AnnualBudgetImportTemplateData {
        expenseCategoryNames = expenseCategoryNames == null ? List.of() : List.copyOf(expenseCategoryNames);
        participantLabels = participantLabels == null ? List.of() : List.copyOf(participantLabels);
    }
}
