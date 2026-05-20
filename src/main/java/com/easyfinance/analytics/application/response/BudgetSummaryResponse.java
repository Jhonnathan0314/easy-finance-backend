package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record BudgetSummaryResponse(
        Long accountId,
        Integer year,
        Integer month,
        Long budgetId,
        BigDecimal expectedAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        Long impactsCount,
        Long paidImpactsCount,
        Long activeImpactsCount,
        Long subBudgetsCount
) {
}
