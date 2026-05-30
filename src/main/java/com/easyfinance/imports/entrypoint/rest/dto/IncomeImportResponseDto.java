package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record IncomeImportResponseDto(
        int createdCount,
        List<IncomeImportRowResponseDto> rows
) {
}

