package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.CreateSubBudgetCommand;
import com.easyfinance.budgets.application.response.SubBudgetResponse;

public interface CreateSubBudgetPort {
    SubBudgetResponse createSubBudget(CreateSubBudgetCommand command);
}
