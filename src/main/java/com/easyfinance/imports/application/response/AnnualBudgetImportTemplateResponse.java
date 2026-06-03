package com.easyfinance.imports.application.response;

public record AnnualBudgetImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}

