package com.easyfinance.imports.application.command;

import java.io.InputStream;

public record ImportCategoryCommand(
        Long accountId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}

