package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.ImportPaymentMethodCommand;
import com.easyfinance.imports.application.validation.PaymentMethodImportParsedRow;

import java.util.List;

public interface PaymentMethodImportParserPort {

    List<PaymentMethodImportParsedRow> parse(ImportPaymentMethodCommand command);
}

