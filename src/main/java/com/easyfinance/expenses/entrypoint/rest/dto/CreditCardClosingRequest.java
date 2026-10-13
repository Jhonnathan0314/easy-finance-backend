package com.easyfinance.expenses.entrypoint.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingRequest(
        @NotNull Long paymentMethodId,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        List<Long> categoryIds,
        ExpensePaymentStateDto targetState
) {
}
