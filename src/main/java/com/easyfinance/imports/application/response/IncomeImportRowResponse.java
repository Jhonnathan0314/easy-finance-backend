package com.easyfinance.imports.application.response;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeImportRowResponse(
        Integer rowNumber,
        LocalDate incomeDate,
        String description,
        String categoryName,
        Long categoryId,
        BigDecimal amount,
        boolean valid,
        Long createdIncomeId,
        List<String> errors
) {
}
