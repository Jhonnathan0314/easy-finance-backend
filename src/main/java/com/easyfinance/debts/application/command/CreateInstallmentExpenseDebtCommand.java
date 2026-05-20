package com.easyfinance.debts.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateInstallmentExpenseDebtCommand(
        Long accountId,
        Long participantId,
        Long originExpenseId,
        Long categoryId,
        String name,
        String description,
        Money totalAmount,
        Integer installmentCount,
        Money installmentAmount,
        LocalDate firstInstallmentDate,
        String notes
) {
}
