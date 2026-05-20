package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CashflowSummaryResponseDto(
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
