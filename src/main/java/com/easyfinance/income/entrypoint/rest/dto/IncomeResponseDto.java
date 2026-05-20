package com.easyfinance.income.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeResponseDto(
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
