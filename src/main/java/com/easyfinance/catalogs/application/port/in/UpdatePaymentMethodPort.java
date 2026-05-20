package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.command.UpdatePaymentMethodCommand;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;

public interface UpdatePaymentMethodPort {
    PaymentMethodResponse updatePaymentMethod(UpdatePaymentMethodCommand command);
}
