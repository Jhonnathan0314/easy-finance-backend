package com.easyfinance.debts.application.port.out;

public interface ExpenseOriginValidationPort {

    void validateInstallmentOrigin(Long accountId, Long originExpenseId);

    void cancelInstallmentOrigin(Long accountId, Long originExpenseId);
}
