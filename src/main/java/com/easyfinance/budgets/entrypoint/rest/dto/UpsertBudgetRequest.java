package com.easyfinance.budgets.entrypoint.rest.dto;

public record UpsertBudgetRequest(
        String name,
        BudgetStatusDto status
) {
}
