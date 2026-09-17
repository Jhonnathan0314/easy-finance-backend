package com.easyfinance.budgets.application.response;

import java.util.List;

public record SubBudgetForwardPlanResponse(
        List<SubBudgetForwardMonthPlan> months
) {
}
