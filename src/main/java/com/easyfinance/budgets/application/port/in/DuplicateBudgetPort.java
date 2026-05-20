package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.DuplicateBudgetCommand;
import com.easyfinance.budgets.application.response.BudgetDetailResponse;

public interface DuplicateBudgetPort {

    BudgetDetailResponse duplicateBudget(DuplicateBudgetCommand command);
}
