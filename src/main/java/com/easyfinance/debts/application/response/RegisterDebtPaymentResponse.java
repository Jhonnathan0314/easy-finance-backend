package com.easyfinance.debts.application.response;

public record RegisterDebtPaymentResponse(
        DebtPaymentResponse payment,
        DebtResponse debt,
        Long createdExpenseId
) {
    public RegisterDebtPaymentResponse(DebtPaymentResponse payment, DebtResponse debt) {
        this(payment, debt, null);
    }
}
