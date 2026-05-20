package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;

public interface RegisterDebtPaymentPort {

    RegisterDebtPaymentResponse registerDebtPayment(RegisterDebtPaymentCommand command);
}
