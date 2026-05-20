package com.easyfinance.budgets.entrypoint.rest.dto;

import java.time.Instant;

public record BudgetResponseDto(
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
