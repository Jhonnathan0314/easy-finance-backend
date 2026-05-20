package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlySummaryResponse(
        Long accountId,
        Integer year,
        Integer month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netBalance,
        BigDecimal totalDebtRemaining,
        BigDecimal totalDebtPaidInMonth,
        Long activeDebtsCount,
        Long paidDebtsCount,
        BigDecimal budgetExpected,
        BigDecimal budgetPaid,
        BigDecimal budgetPending,
        Instant generatedAt
) {
}
