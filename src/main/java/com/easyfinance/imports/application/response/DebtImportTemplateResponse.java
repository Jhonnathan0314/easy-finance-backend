package com.easyfinance.imports.application.response;

public record DebtImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}
