package com.easyfinance.imports.application.response;

public record PaymentMethodImportTemplateResponse(
        String filename,
        String contentType,
        byte[] content
) {
}

