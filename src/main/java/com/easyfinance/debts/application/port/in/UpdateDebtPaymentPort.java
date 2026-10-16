package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.command.UpdateDebtPaymentCommand;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;

public interface UpdateDebtPaymentPort {

    RegisterDebtPaymentResponse updateDebtPayment(UpdateDebtPaymentCommand command);
}
