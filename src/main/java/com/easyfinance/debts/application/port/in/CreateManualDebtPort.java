package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.response.DebtResponse;

public interface CreateManualDebtPort {
    DebtResponse createManualDebt(CreateManualDebtCommand command);
}
