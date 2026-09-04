package com.easyfinance.imports.application.command;

import java.io.InputStream;

public record ImportDebtCommand(
        Long accountId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
) {
}
