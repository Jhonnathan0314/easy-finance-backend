package com.easyfinance.imports.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;
import java.util.List;

public record ExpenseImportBatch(
        Long id,
        Long accountId,
        Long participantId,
        String originalFilename,
        ExpenseImportStatus status,
        Integer totalRows,
        Integer validRows,
        Integer invalidRows,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ExpenseImportRow> rows
) {
    public ExpenseImportBatch {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public static ExpenseImportBatch preview(
            Long accountId,
            Long participantId,
            String originalFilename,
            List<ExpenseImportRow> rows
    ) {
        int total = rows.size();
        int valid = (int) rows.stream().filter(ExpenseImportRow::valid).count();
        return new ExpenseImportBatch(null, accountId, participantId, originalFilename, ExpenseImportStatus.PREVIEW, total, valid, total - valid, null, null, null, rows);
    }

    public ExpenseImportBatch confirm(Instant confirmedAt) {
        if (status == ExpenseImportStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("IMPORT_ALREADY_CONFIRMED", "Import batch is already confirmed.");
        }
        if (status != ExpenseImportStatus.PREVIEW) {
            throw new BusinessRuleViolationException("IMPORT_NOT_CONFIRMABLE", "Import batch cannot be confirmed.");
        }
        if (validRows == null || validRows == 0) {
            throw new BusinessRuleViolationException("IMPORT_NO_VALID_ROWS", "Import batch has no valid rows.");
        }
        return new ExpenseImportBatch(id, accountId, participantId, originalFilename, ExpenseImportStatus.CONFIRMED, totalRows, validRows, invalidRows, confirmedAt, createdAt, updatedAt, rows);
    }
}
