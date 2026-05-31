package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.PaymentMethodImportTemplateResponse;

public interface GeneratePaymentMethodImportTemplatePort {

    PaymentMethodImportTemplateResponse generate(Long accountId);
}

