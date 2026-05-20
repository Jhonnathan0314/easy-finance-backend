package com.easyfinance.budgets.application.response;

import java.util.List;

public record BudgetDetailResponse(
        BudgetResponse budget,
        List<SubBudgetResponse> subBudgets,
        List<BudgetImpactResponse> impacts
) {
}
