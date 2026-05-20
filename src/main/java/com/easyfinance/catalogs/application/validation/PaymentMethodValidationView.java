package com.easyfinance.catalogs.application.validation;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;

public record PaymentMethodValidationView(
        Long id,
        Long accountId,
        PaymentMethodType type,
        CatalogStatus status
) {
}
