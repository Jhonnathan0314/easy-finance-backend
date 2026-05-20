package com.easyfinance.debts.application.command;

import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record RegisterDebtPaymentCommand(
        Long accountId,
        Long debtId,
        DebtPaymentType paymentType,
        Money amount,
        LocalDate paymentDate,
        String notes
) {
}
