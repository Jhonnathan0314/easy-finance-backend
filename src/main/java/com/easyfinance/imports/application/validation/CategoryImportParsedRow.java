package com.easyfinance.imports.application.validation;

import com.easyfinance.catalogs.domain.model.CategoryType;

import java.util.List;

public record CategoryImportParsedRow(
        Integer rowNumber,
        String name,
        String description,
        CategoryType type,
        List<String> errors
) {
}
