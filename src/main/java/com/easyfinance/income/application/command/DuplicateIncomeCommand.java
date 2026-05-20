package com.easyfinance.income.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record DuplicateIncomeCommand(
        Long accountId,
        Long incomeId,
        LocalDate incomeDate,
        Money amount,
        String description
) {
}
