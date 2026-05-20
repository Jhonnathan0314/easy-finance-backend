package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record CreateDebtBudgetImpactsCommand(
        Long accountId,
        Long debtId,
        Long expenseId,
        Long categoryId,
        String debtName,
        Money totalAmount,
        Integer installmentCount,
        Money installmentAmount,
        LocalDate firstInstallmentDate
) {
}
