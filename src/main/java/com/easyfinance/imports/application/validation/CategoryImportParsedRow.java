package com.easyfinance.imports.application.validation;

import com.easyfinance.catalogs.domain.model.CategoryType;

import java.util.List;

public record CategoryImportParsedRow(
        Integer rowNumber,
        String name,
        String description,
        CategoryType type,
        String status,
        List<String> errors
) {
    public CategoryImportParsedRow(Integer rowNumber, String name, String description, CategoryType type, List<String> errors) { this(rowNumber,name,description,type,null,errors); }
}
