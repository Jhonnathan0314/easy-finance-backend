package com.easyfinance.budgets.application.command;

import com.easyfinance.budgets.domain.model.BudgetStatus;

public record UpsertBudgetCommand(
        Long accountId,
        Integer year,
        Integer month,
        String name,
        BudgetStatus status
) {
}
