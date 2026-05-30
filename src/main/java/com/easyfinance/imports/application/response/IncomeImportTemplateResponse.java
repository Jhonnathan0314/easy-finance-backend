package com.easyfinance.imports.application.response;

public record IncomeImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}

