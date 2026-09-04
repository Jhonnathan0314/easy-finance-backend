package com.easyfinance.imports.application.template;

import java.util.List;

public record DebtImportTemplateData(
        List<String> participantLabels
) {
    public DebtImportTemplateData {
        participantLabels = participantLabels == null ? List.of() : List.copyOf(participantLabels);
    }
}
