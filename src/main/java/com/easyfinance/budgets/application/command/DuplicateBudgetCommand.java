package com.easyfinance.budgets.application.command;

public record DuplicateBudgetCommand(
        Long accountId,
        Integer sourceYear,
        Integer sourceMonth,
        Integer targetYear,
        Integer targetMonth,
        String name
) {
}
