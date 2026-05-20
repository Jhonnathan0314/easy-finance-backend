package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;

public interface ListPaymentMethodsPort {
    PageResponse<PaymentMethodResponse> listPaymentMethods(ListPaymentMethodsQuery query);
}
