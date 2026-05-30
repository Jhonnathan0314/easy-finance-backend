package com.easyfinance.budgets.entrypoint.rest.dto;

import java.util.List;

public record AnnualBudgetResponseDto(
        Long accountId,
        Integer year,
        List<BudgetResponseDto> createdBudgets
) {
}

