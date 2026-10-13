package com.easyfinance.expenses.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardClosingResultResponseDto(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long updatedCount,
        String targetState
) {
    public CreditCardClosingResultResponseDto(Long paymentMethodId, LocalDate from, LocalDate to, BigDecimal totalAmount, long updatedCount) {
        this(paymentMethodId, from, to, totalAmount, updatedCount, "PAID");
    }
}
