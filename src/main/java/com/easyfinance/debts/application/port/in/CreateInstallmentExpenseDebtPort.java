package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.response.DebtResponse;

public interface CreateInstallmentExpenseDebtPort {
    DebtResponse createInstallmentExpenseDebt(CreateInstallmentExpenseDebtCommand command);
}
