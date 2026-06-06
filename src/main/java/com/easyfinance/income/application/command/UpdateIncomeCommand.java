package com.easyfinance.income.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record UpdateIncomeCommand(
        Long accountId,
        Long incomeId,
        Long participantId,
        Long categoryId,
        String description,
        Money amount,
        LocalDate incomeDate
) {
}
