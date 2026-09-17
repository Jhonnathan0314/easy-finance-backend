package com.easyfinance.budgets.application.response;

import java.util.List;

public record SubBudgetForwardApplyResponse(
        List<SubBudgetForwardMonthResult> months
) {
}
