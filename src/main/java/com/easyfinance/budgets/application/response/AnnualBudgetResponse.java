package com.easyfinance.budgets.application.response;

import java.util.List;

public record AnnualBudgetResponse(
        Long accountId,
        Integer year,
        List<BudgetResponse> createdBudgets
) {
}

