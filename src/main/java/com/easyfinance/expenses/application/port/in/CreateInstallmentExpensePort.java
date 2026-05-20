package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface CreateInstallmentExpensePort {
    ExpenseResponse createInstallmentExpense(CreateInstallmentExpenseCommand command);
}
