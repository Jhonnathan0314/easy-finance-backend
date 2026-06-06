package com.easyfinance.debts.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateManualDebtCommand(
        Long accountId,
        Long participantId,
        String name,
        String description,
        Money totalAmount,
        Integer installmentCount,
        Money installmentAmount,
        LocalDate startDate,
        LocalDate dueDate,
        String notes
) {
}
