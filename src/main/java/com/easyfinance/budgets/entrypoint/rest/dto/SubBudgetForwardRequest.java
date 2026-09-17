package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubBudgetForwardRequest(
        @NotNull SubBudgetForwardActionDto action,
        Long subBudgetId,
        Long categoryId,
        Long participantId,
        String name,
        BigDecimal plannedAmount
) {
}
