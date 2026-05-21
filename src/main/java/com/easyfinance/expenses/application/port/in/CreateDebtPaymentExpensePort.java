package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;

public interface CreateDebtPaymentExpensePort {

    ExpenseResponse createDebtPaymentExpense(CreateDebtPaymentExpenseCommand command);
}
