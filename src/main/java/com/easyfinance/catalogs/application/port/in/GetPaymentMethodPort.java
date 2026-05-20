package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.response.PaymentMethodResponse;

public interface GetPaymentMethodPort {
    PaymentMethodResponse getPaymentMethod(Long accountId, Long paymentMethodId);
}
