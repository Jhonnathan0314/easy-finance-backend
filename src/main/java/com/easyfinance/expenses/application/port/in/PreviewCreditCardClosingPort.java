package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;

public interface PreviewCreditCardClosingPort {

    CreditCardClosingPreviewResponse previewClosing(CreditCardClosingCommand command);
}
