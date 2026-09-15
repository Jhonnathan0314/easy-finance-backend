package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record ApplyDebtPaymentImpactCommand(
        Long accountId,
        Long debtId,
        Money amount,
        boolean debtFullySettled,
        LocalDate settlementDate
) {
}
