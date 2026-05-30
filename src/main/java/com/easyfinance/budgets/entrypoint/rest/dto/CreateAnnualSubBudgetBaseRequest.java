package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAnnualSubBudgetBaseRequest(
        @NotBlank String name,
        Long categoryId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal plannedAmount
) {
}

