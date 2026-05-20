package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface GetExpensePort {
    ExpenseResponse getExpense(Long accountId, Long expenseId);
}
