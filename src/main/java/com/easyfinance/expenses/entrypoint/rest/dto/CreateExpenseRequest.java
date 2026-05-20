package com.easyfinance.expenses.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotNull Long categoryId,
        @NotNull Long paymentMethodId,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate expenseDate,
        ExpensePaymentStateDto paymentState
) {
}
