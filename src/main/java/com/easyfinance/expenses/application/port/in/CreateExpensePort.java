package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreateExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface CreateExpensePort {
    ExpenseResponse createExpense(CreateExpenseCommand command);
}
