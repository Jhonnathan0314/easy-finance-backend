package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record BudgetSummaryResponseDto(
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
