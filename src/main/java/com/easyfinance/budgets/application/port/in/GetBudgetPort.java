package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.response.BudgetDetailResponse;

public interface GetBudgetPort {
    BudgetDetailResponse getBudget(Long accountId, Integer year, Integer month);
}
