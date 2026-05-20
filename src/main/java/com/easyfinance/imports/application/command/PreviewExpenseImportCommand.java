package com.easyfinance.imports.application.command;

import java.io.InputStream;

public record PreviewExpenseImportCommand(
        Long accountId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}
