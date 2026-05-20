package com.easyfinance.debts.application.response;

public record RegisterDebtPaymentResponse(
        DebtPaymentResponse payment,
        DebtResponse debt
) {
}
