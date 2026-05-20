package com.easyfinance.catalogs.application.validation;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;

public record CategoryValidationView(
        Long id,
        Long accountId,
        CategoryType type,
        CatalogStatus status
) {
}
