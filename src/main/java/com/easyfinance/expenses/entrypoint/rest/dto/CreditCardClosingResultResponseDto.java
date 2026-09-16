package com.easyfinance.expenses.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardClosingResultResponseDto(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long updatedCount
) {
}
