package com.easyfinance.catalogs.application.query;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.shared.application.PageQuery;

public record ListPaymentMethodsQuery(
        Long accountId,
        PaymentMethodType type,
        CatalogStatus status,
        String search,
        PageQuery pageQuery,
        String sort
) {
    public ListPaymentMethodsQuery {
        search = normalizeSearch(search);
    }

    private static String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
