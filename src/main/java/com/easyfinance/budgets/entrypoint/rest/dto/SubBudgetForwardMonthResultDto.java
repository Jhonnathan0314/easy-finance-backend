package com.easyfinance.budgets.entrypoint.rest.dto;

public record SubBudgetForwardMonthResultDto(
        Integer year,
        Integer month,
        String outcome,
        String reason
) {
}
