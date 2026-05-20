package com.easyfinance.income.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeResponse(
        Long id,
        Long accountId,
        Long categoryId,
        Long participantId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate incomeDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
