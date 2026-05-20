package com.easyfinance.budgets.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SubBudgetResponseDto(
        Long id,
        Long accountId,
        Long budgetId,
        Long categoryId,
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
