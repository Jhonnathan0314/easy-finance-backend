package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.UpsertBudgetCommand;
import com.easyfinance.budgets.application.response.BudgetResponse;

public interface UpsertBudgetPort {
    BudgetResponse upsertBudget(UpsertBudgetCommand command);
}
