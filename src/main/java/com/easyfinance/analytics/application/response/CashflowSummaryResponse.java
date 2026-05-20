package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CashflowSummaryResponse(
        Long accountId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalSimpleExpenseOutflow,
        BigDecimal totalDebtPaymentOutflow,
        BigDecimal totalOutflow,
        BigDecimal netCashflow,
        Instant generatedAt
) {
}
