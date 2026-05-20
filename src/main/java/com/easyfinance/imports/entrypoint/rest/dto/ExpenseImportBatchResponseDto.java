package com.easyfinance.imports.entrypoint.rest.dto;

import java.time.Instant;
import java.util.List;

public record ExpenseImportBatchResponseDto(
        Long batchId,
        Long accountId,
        Long participantId,
        String originalFilename,
        String status,
        Integer totalRows,
        Integer validRows,
        Integer invalidRows,
        Instant confirmedAt,
        List<ExpenseImportRowResponseDto> rows
) {
}
