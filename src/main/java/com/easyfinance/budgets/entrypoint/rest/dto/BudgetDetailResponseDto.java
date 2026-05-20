package com.easyfinance.budgets.entrypoint.rest.dto;

import java.util.List;

public record BudgetDetailResponseDto(
        BudgetResponseDto budget,
        List<SubBudgetResponseDto> subBudgets,
        List<BudgetImpactResponseDto> impacts
) {
}
