package com.easyfinance.budgets.application.response;

import java.time.Instant;

public record BudgetResponse(
        Long id,
        Long accountId,
        Integer year,
        Integer month,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
