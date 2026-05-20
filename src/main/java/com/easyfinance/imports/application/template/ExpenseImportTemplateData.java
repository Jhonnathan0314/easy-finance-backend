package com.easyfinance.imports.application.template;

import java.util.List;

public record ExpenseImportTemplateData(
        List<String> categoryNames,
        List<String> paymentMethodNames
) {

    public ExpenseImportTemplateData {
        categoryNames = categoryNames == null ? List.of() : List.copyOf(categoryNames);
        paymentMethodNames = paymentMethodNames == null ? List.of() : List.copyOf(paymentMethodNames);
    }
}
