package com.easyfinance.catalogs.application.command;

import com.easyfinance.catalogs.domain.model.CategoryType;

public record CreateCategoryCommand(
        Long accountId,
        String name,
        String description,
        CategoryType type
) {
}
