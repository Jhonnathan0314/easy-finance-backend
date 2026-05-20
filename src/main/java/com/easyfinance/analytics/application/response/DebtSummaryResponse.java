package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record DebtSummaryResponse(
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
