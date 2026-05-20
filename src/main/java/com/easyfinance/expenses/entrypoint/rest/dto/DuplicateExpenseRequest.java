package com.easyfinance.expenses.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DuplicateExpenseRequest(
        @NotNull LocalDate expenseDate,
        @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(max = 500) String description,
        ExpensePaymentStateDto paymentState
) {
}
