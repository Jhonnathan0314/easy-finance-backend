package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record IncomeImportRowResponseDto(
        Integer rowNumber,
        boolean valid,
        Long createdIncomeId,
        List<String> errors
) {
}

