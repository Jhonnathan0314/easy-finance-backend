package com.easyfinance.expenses.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
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
        Instant createdAt,
        Instant updatedAt
) {
}
