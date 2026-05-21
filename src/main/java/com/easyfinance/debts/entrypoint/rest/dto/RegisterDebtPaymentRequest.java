package com.easyfinance.debts.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterDebtPaymentRequest(
        @NotNull DebtPaymentTypeDto paymentType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @Size(max = 1000) String notes,
        Boolean createExpense,
        Long categoryId,
        Long paymentMethodId,
        @Size(max = 500) String expenseDescription
) {
}
