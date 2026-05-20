package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseSummaryResponseDto(
        Long accountId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalSimpleExpenses,
        BigDecimal totalInstallmentPurchases,
        BigDecimal totalExpensesConceptual,
        Long expensesCount,
        Instant generatedAt
) {
}
