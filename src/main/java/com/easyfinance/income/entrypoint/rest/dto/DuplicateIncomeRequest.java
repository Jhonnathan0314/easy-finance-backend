package com.easyfinance.income.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DuplicateIncomeRequest(
        @NotNull LocalDate incomeDate,
        @DecimalMin(value = "0.01") BigDecimal amount,
        String description
) {
}
