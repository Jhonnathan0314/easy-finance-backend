package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.command.CreatePaymentMethodCommand;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;

public interface CreatePaymentMethodPort {
    PaymentMethodResponse createPaymentMethod(CreatePaymentMethodCommand command);
}
