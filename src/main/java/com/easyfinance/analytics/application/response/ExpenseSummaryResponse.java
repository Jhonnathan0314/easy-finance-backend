package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseSummaryResponse(
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
