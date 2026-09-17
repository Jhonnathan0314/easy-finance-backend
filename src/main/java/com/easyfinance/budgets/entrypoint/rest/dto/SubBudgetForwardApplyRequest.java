package com.easyfinance.budgets.entrypoint.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SubBudgetForwardApplyRequest(
        @NotNull SubBudgetForwardActionDto action,
        Long subBudgetId,
        Long categoryId,
        Long participantId,
        String name,
        BigDecimal plannedAmount,
        List<Integer> months
) {
}
