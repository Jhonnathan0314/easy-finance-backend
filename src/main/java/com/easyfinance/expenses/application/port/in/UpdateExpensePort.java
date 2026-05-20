package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.UpdateExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface UpdateExpensePort {
    ExpenseResponse updateExpense(UpdateExpenseCommand command);
}
