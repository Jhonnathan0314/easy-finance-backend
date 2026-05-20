package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlySummaryResponseDto(
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
