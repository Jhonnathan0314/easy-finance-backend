package com.easyfinance.catalogs.application.command;

import com.easyfinance.catalogs.domain.model.CategoryType;

public record UpdateCategoryCommand(
        Long accountId,
        Long categoryId,
        String name,
        String description,
        CategoryType type
) {
}
