package com.easyfinance.imports.application.response;

import java.time.Instant;
import java.util.List;

public record ExpenseImportBatchResponse(
        Long batchId,
        Long accountId,
        Long participantId,
        String originalFilename,
        String status,
        Integer totalRows,
        Integer validRows,
        Integer invalidRows,
        Instant confirmedAt,
        List<ExpenseImportRowResponse> rows
) {
}
