package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record CategoryImportResponseDto(
        int createdCount,
        List<CategoryImportRowResponseDto> rows
) {
}

