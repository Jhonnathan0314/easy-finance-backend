package com.easyfinance.debts.entrypoint.rest.dto;

public record RegisterDebtPaymentResponseDto(
        DebtPaymentResponseDto payment,
        DebtResponseDto debt,
        Long createdExpenseId
) {
}
