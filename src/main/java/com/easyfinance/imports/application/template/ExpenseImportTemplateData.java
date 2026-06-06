package com.easyfinance.imports.application.template;

import java.util.List;

public record ExpenseImportTemplateData(
        List<String> categoryNames,
        List<String> paymentMethodNames,
        List<DebtOption> debtOptions,
        List<String> participantLabels
) {

    public ExpenseImportTemplateData {
        categoryNames = categoryNames == null ? List.of() : List.copyOf(categoryNames);
        paymentMethodNames = paymentMethodNames == null ? List.of() : List.copyOf(paymentMethodNames);
        debtOptions = debtOptions == null ? List.of() : List.copyOf(debtOptions);
        participantLabels = participantLabels == null ? List.of() : List.copyOf(participantLabels);
    }

    public ExpenseImportTemplateData(List<String> categoryNames, List<String> paymentMethodNames) {
        this(categoryNames, paymentMethodNames, List.of(), List.of());
    }

    public ExpenseImportTemplateData(List<String> categoryNames, List<String> paymentMethodNames, List<DebtOption> debtOptions) {
        this(categoryNames, paymentMethodNames, debtOptions, List.of());
    }

    public record DebtOption(
            Long debtId,
            String label
    ) {
    }
}
