package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

public record CreateSubBudgetCommand(
        Long accountId,
        Long budgetId,
        Long categoryId,
        String name,
        Money plannedAmount
) {
}
