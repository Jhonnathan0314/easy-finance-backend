package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.UpdateSubBudgetCommand;
import com.easyfinance.budgets.application.response.SubBudgetResponse;

public interface UpdateSubBudgetPort {
    SubBudgetResponse updateSubBudget(UpdateSubBudgetCommand command);
}
