package com.easyfinance.imports.application.template;

import java.util.List;

public record IncomeImportTemplateData(
        List<String> categoryNames,
        List<String> participantLabels
) {
    public IncomeImportTemplateData {
        categoryNames = categoryNames == null ? List.of() : List.copyOf(categoryNames);
        participantLabels = participantLabels == null ? List.of() : List.copyOf(participantLabels);
    }
}
