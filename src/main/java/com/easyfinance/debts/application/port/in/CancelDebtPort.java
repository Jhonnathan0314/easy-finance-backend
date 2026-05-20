package com.easyfinance.debts.application.port.in;

public interface CancelDebtPort {
    void cancelDebt(Long accountId, Long debtId);
}
