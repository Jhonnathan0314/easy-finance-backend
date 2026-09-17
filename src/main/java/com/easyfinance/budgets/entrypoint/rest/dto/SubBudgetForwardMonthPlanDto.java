package com.easyfinance.budgets.entrypoint.rest.dto;

import java.math.BigDecimal;

public record SubBudgetForwardMonthPlanDto(
        Integer year,
        Integer month,
        Long budgetId,
        String status,
        Long currentSubBudgetId,
        String currentName,
        Long currentCategoryId,
        Long currentParticipantId,
        BigDecimal currentPlannedAmount,
        String proposedName,
        Long proposedCategoryId,
        Long proposedParticipantId,
        BigDecimal proposedPlannedAmount
) {
}
