package com.easyfinance.debts.application.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtPaymentResponse(
        Long id,
        Long accountId,
        Long debtId,
        Long participantId,
        String paymentType,
        BigDecimal amount,
        BigDecimal capitalAmount,
        BigDecimal interestAmount,
        String currency,
        LocalDate paymentDate,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
