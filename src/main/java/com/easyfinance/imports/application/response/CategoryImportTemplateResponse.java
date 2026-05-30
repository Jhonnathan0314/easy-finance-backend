package com.easyfinance.imports.application.response;

public record CategoryImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}

