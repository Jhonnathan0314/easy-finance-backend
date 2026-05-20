package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.response.DebtPaymentResponse;

public interface GetDebtPaymentPort {

    DebtPaymentResponse getDebtPayment(Long accountId, Long debtId, Long paymentId);
}
