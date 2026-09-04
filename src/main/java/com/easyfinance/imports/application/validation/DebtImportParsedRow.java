package com.easyfinance.imports.application.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DebtImportParsedRow(
        Integer rowNumber,
        String name,
        String description,
        BigDecimal totalAmount,
        BigDecimal remainingBalance,
        Integer installmentCount,
        BigDecimal installmentAmount,
        LocalDate startDate,
        LocalDate dueDate,
        String participantLabel,
        String notes,
        List<String> errors
) {
    public DebtImportParsedRow {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
