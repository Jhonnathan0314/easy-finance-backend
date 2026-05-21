package com.easyfinance.expenses.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponseDto(
        Long id,
        Long accountId,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String paymentState,
        String status,
        String expenseType,
        String sourceType,
        Long sourceDebtPaymentId,
        Instant createdAt,
        Instant updatedAt
) {
}
