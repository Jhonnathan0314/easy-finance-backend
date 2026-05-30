package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateAnnualBudgetRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        String name,
        BudgetStatusDto status,
        @Valid List<CreateAnnualSubBudgetBaseRequest> subBudgets
) {
}

