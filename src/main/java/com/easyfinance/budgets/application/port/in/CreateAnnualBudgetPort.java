package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.CreateAnnualBudgetCommand;
import com.easyfinance.budgets.application.response.AnnualBudgetResponse;

public interface CreateAnnualBudgetPort {
    AnnualBudgetResponse createAnnualBudget(CreateAnnualBudgetCommand command);
}

