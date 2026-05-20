package com.easyfinance.expenses.application.port.in;

public interface CancelExpensePort {
    void cancelExpense(Long accountId, Long expenseId);
}
