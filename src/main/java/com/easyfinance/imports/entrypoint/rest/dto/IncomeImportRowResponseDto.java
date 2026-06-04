package com.easyfinance.imports.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeImportRowResponseDto(
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
