package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

public record CreateAnnualSubBudgetBaseCommand(
        String name,
        Long categoryId,
        Money plannedAmount
) {
}

