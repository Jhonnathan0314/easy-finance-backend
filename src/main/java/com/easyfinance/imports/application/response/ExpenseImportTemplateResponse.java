package com.easyfinance.imports.application.response;

public record ExpenseImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}
