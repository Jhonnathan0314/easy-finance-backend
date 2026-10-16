package com.easyfinance.debts.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtPaymentResponseDto(
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
        Instant updatedAt,
        boolean hasAssociatedExpense
) {
    public DebtPaymentResponseDto(
            Long id, Long accountId, Long debtId, Long participantId, String paymentType, BigDecimal amount,
            BigDecimal capitalAmount, BigDecimal interestAmount, String currency, LocalDate paymentDate,
            String notes, String status, Instant createdAt, Instant updatedAt
    ) {
        this(id, accountId, debtId, participantId, paymentType, amount, capitalAmount, interestAmount, currency, paymentDate, notes, status, createdAt, updatedAt, false);
    }
}
