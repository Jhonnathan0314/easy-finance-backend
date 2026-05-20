package com.easyfinance.budgets.application.port.in;

public interface DeactivateSubBudgetPort {
    void deactivateSubBudget(Long accountId, Long budgetId, Long subBudgetId);
}
