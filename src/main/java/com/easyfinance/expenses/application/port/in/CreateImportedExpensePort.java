package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreateImportedExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface CreateImportedExpensePort {

    ExpenseResponse createImportedExpense(CreateImportedExpenseCommand command);
}
