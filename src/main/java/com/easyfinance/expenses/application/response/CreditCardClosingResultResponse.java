package com.easyfinance.expenses.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardClosingResultResponse(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long updatedCount
) {
}
