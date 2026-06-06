package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

public record UpdateSubBudgetCommand(
        Long accountId,
        Long budgetId,
        Long subBudgetId,
        Long categoryId,
        Long participantId,
        String name,
        Money plannedAmount
) {
}
