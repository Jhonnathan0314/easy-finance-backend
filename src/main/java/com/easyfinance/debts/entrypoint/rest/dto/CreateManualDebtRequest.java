package com.easyfinance.debts.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateManualDebtRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @Min(1) Integer installmentCount,
        @DecimalMin(value = "0.01") BigDecimal installmentAmount,
        @NotNull LocalDate startDate,
        LocalDate dueDate,
        @Size(max = 1000) String notes
) {
}
