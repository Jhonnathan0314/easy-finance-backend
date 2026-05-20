package com.easyfinance.budgets.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetImpactResponseDto(
        Long id,
        Long accountId,
        Long budgetId,
        Long subBudgetId,
        Long debtId,
        Long expenseId,
        Integer periodYear,
        Integer periodMonth,
        BigDecimal expectedAmount,
        String expectedCurrency,
        BigDecimal paidAmount,
        String paidCurrency,
        String status,
        String sourceType,
        Instant createdAt,
        Instant updatedAt
) {
}
