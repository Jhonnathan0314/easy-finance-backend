package com.easyfinance.imports.entrypoint.rest.dto;

import java.util.List;

public record CategoryImportRowResponseDto(
        Integer rowNumber,
        boolean valid,
        Long createdCategoryId,
        List<String> errors
) {
}

