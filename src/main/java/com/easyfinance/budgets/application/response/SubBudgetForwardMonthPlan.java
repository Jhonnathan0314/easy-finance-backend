package com.easyfinance.budgets.application.response;

import java.math.BigDecimal;

public record SubBudgetForwardMonthPlan(
        Integer year,
        Integer month,
        Long budgetId,
        SubBudgetForwardMonthStatus status,
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
