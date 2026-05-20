package com.easyfinance.catalogs.application.port.in;

public interface DeactivatePaymentMethodPort {
    void deactivatePaymentMethod(Long accountId, Long paymentMethodId);
}
