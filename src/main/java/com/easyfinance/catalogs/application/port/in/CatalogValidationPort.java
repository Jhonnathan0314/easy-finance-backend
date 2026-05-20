package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;

import java.util.Optional;

public interface CatalogValidationPort {

    Optional<CategoryValidationView> findCategoryForValidation(Long accountId, Long categoryId);

    Optional<CategoryValidationView> findCategoryForValidation(Long accountId, String normalizedName);

    Optional<PaymentMethodValidationView> findPaymentMethodForValidation(Long accountId, Long paymentMethodId);

    Optional<PaymentMethodValidationView> findPaymentMethodForValidation(Long accountId, String normalizedName);
}
