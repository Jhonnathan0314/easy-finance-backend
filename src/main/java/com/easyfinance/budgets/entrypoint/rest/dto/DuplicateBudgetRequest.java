package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DuplicateBudgetRequest(
        @NotNull @Min(2000) @Max(2100) Integer targetYear,
        @NotNull @Min(1) @Max(12) Integer targetMonth,
        String name
) {
}
