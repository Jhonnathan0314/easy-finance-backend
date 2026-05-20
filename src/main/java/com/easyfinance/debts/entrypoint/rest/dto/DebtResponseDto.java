package com.easyfinance.debts.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtResponseDto(
        Long id,
        Long accountId,
        Long participantId,
        Long originExpenseId,
        String sourceType,
        String name,
        String description,
        BigDecimal totalAmount,
        String totalCurrency,
        BigDecimal remainingAmount,
        String remainingCurrency,
        Integer installmentCount,
        BigDecimal installmentAmount,
        String installmentCurrency,
        LocalDate startDate,
        LocalDate endDate,
        String state,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
