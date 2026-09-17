package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

public record PreviewSubBudgetForwardCommand(
        Long accountId,
        Long budgetId,
        SubBudgetForwardAction action,
        Long subBudgetId,
        Long categoryId,
        Long participantId,
        String name,
        Money plannedAmount
) {
}
