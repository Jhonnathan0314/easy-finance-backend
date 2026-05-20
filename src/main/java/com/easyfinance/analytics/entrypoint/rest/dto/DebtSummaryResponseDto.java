package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record DebtSummaryResponseDto(
        Long accountId,
        Long activeDebtsCount,
        Long paidDebtsCount,
        Long cancelledDebtsCount,
        BigDecimal totalDebtAmount,
        BigDecimal totalRemainingBalance,
        BigDecimal totalPaidAmount,
        Long manualDebtsCount,
        Long installmentExpenseDebtsCount
) {
}
