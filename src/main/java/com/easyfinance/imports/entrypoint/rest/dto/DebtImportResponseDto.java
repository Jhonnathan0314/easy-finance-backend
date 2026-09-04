package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record DebtImportResponseDto(
        int createdCount,
        List<DebtImportRowResponseDto> rows
) {
}
