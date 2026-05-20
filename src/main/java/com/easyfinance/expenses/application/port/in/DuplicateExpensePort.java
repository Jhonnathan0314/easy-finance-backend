package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.DuplicateExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface DuplicateExpensePort {
    ExpenseResponse duplicateExpense(DuplicateExpenseCommand command);
}
