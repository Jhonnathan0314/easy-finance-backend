package com.easyfinance.budgets.application.response;

public record SubBudgetForwardMonthResult(
        Integer year,
        Integer month,
        String outcome,
        String reason
) {
}
