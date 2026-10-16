package com.easyfinance.debts.application.command;

import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.shared.domain.Money;

import java.time.LocalDate;

public record UpdateDebtPaymentCommand(
        Long accountId,
        Long debtId,
        Long paymentId,
        DebtPaymentType paymentType,
        Money capitalAmount,
        Money interestAmount,
        LocalDate paymentDate,
        String notes
) {
}
