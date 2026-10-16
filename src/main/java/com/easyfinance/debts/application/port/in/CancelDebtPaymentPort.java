package com.easyfinance.debts.application.port.in;

public interface CancelDebtPaymentPort {

    void cancelDebtPayment(Long accountId, Long debtId, Long paymentId);
}
