package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

import java.util.List;

public record ApplySubBudgetForwardCommand(
        Long accountId,
        Long budgetId,
        SubBudgetForwardAction action,
        Long subBudgetId,
        Long categoryId,
        Long participantId,
        String name,
        Money plannedAmount,
        List<Integer> months
) {
}
