package com.easyfinance.imports.application.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeImportParsedRow(
        Integer rowNumber,
        LocalDate incomeDate,
        String description,
        String categoryName,
        String participantLabel,
        BigDecimal amount,
        List<String> errors
) {
    public IncomeImportParsedRow {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
