package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportPaymentMethodCommand;
import com.easyfinance.imports.application.response.PaymentMethodImportResponse;

public interface ImportPaymentMethodPort {

    PaymentMethodImportResponse importPaymentMethods(ImportPaymentMethodCommand command);
}

