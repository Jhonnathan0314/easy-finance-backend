package com.easyfinance.budgets.application.response;

import java.math.BigDecimal;
import java.time.Instant;

public record SubBudgetResponse(
        Long id,
        Long accountId,
        Long budgetId,
        Long categoryId,
        Long participantId,
        Long debtId,
        String name,
        BigDecimal plannedAmount,
        String plannedCurrency,
        BigDecimal spentAmount,
        String spentCurrency,
        String status,
        String sourceType,
        Instant createdAt,
        Instant updatedAt
) {
}
