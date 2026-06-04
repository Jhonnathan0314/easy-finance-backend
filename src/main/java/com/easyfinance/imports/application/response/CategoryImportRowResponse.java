package com.easyfinance.imports.application.response;

import com.easyfinance.catalogs.domain.model.CategoryType;

import java.util.List;

public record CategoryImportRowResponse(
        Integer rowNumber,
        String name,
        String description,
        CategoryType type,
        boolean valid,
        Long createdCategoryId,
        List<String> errors
) {
}
