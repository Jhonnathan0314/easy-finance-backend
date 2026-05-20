package com.easyfinance.budgets.application.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetImpactResponse(
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
