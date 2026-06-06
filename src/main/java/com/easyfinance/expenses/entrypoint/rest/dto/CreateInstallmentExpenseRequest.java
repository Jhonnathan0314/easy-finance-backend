package com.easyfinance.expenses.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInstallmentExpenseRequest(
        @NotNull Long categoryId,
        Long participantId,
        @NotNull Long paymentMethodId,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @NotNull LocalDate expenseDate,
        @NotNull @Min(1) Integer installmentCount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal installmentAmount,
        @NotNull LocalDate firstInstallmentDate,
        @Size(max = 150) String debtName,
        @Size(max = 1000) String notes
) {
}
