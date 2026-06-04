package com.easyfinance.imports.entrypoint.rest.dto;

import com.easyfinance.catalogs.domain.model.CategoryType;

import java.util.List;

public record CategoryImportRowResponseDto(
        Integer rowNumber,
        String name,
        String description,
        CategoryType type,
        boolean valid,
        Long createdCategoryId,
        List<String> errors
) {
}
