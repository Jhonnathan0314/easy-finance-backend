package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;

public interface ConfirmCreditCardClosingPort {

    CreditCardClosingResultResponse confirmClosing(CreditCardClosingCommand command);
}
