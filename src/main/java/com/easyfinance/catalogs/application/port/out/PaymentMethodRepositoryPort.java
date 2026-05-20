package com.easyfinance.catalogs.application.port.out;

import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.domain.model.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepositoryPort {

    PaymentMethod save(PaymentMethod paymentMethod);

    Optional<PaymentMethod> findByAccountIdAndId(Long accountId, Long paymentMethodId);

    Optional<PaymentMethod> findByAccountIdAndNormalizedName(Long accountId, String normalizedName);

    boolean existsActiveByAccountIdAndNormalizedName(Long accountId, String normalizedName);

    boolean existsActiveByAccountIdAndNormalizedNameAndIdNot(Long accountId, String normalizedName, Long id);

    PageResponse<PaymentMethod> findAll(ListPaymentMethodsQuery query);

    List<PaymentMethod> findActiveByAccountId(Long accountId);
}
