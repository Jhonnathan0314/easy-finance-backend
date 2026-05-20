package com.easyfinance.imports.application.template;

import java.util.List;

public record ExpenseImportTemplateData(
        List<String> categoryNames,
        List<String> paymentMethodNames,
        List<DebtOption> debtOptions
) {

    public ExpenseImportTemplateData {
        categoryNames = categoryNames == null ? List.of() : List.copyOf(categoryNames);
        paymentMethodNames = paymentMethodNames == null ? List.of() : List.copyOf(paymentMethodNames);
        debtOptions = debtOptions == null ? List.of() : List.copyOf(debtOptions);
    }

    public ExpenseImportTemplateData(List<String> categoryNames, List<String> paymentMethodNames) {
        this(categoryNames, paymentMethodNames, List.of());
    }

    public record DebtOption(
            Long debtId,
            String label
    ) {
    }
}
