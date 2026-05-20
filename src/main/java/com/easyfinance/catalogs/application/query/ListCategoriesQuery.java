package com.easyfinance.catalogs.application.query;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.shared.application.PageQuery;

public record ListCategoriesQuery(
        Long accountId,
        CategoryType type,
        CatalogStatus status,
        String search,
        PageQuery pageQuery,
        String sort
) {
    public ListCategoriesQuery {
        search = normalizeSearch(search);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
