package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSubBudgetRequest(
        Long categoryId,
        Long participantId,
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal plannedAmount
) {
}
