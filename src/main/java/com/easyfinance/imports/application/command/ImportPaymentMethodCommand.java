package com.easyfinance.imports.application.command;

import java.io.InputStream;

public record ImportPaymentMethodCommand(
        Long accountId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}

