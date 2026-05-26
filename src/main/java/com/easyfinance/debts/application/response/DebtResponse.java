package com.easyfinance.debts.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtResponse(
        Long id,
        Long accountId,
        Long participantId,
        Long originExpenseId,
        String sourceType,
        String name,
        String description,
        BigDecimal totalAmount,
        BigDecimal scheduledTotalAmount,
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
