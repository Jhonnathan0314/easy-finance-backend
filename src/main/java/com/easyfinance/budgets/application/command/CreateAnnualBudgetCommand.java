package com.easyfinance.budgets.application.command;

import com.easyfinance.budgets.domain.model.BudgetStatus;

import java.util.List;

public record CreateAnnualBudgetCommand(
        Long accountId,
        Integer year,
        String name,
        BudgetStatus status,
        List<CreateAnnualSubBudgetBaseCommand> subBudgets
) {
}

