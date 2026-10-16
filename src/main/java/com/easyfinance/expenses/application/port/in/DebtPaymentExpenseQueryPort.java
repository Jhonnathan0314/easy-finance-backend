package com.easyfinance.expenses.application.port.in;

public interface DebtPaymentExpenseQueryPort {

    boolean hasActiveExpenseForDebtPayment(Long accountId, Long debtPaymentId);
}
