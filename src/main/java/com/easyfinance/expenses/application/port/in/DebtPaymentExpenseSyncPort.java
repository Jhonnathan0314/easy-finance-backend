package com.easyfinance.expenses.application.port.in;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public interface DebtPaymentExpenseSyncPort {

    /** Updates amount and date of the active expense associated with a debt payment, if any. */
    void updateExpenseForDebtPayment(Long accountId, Long debtPaymentId, Money amount, LocalDate expenseDate);

    /** Cancels the active expense associated with a debt payment, if any. */
    void cancelExpenseForDebtPayment(Long accountId, Long debtPaymentId);
}
