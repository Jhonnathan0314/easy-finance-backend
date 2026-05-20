package com.easyfinance.budgets.application.command;

import com.easyfinance.shared.domain.Money;

public record ApplyDebtPaymentImpactCommand(
        Long accountId,
        Long debtId,
        Money amount
) {
}
