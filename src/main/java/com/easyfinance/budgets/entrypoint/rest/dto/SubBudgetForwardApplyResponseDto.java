package com.easyfinance.budgets.entrypoint.rest.dto;

import java.util.List;

public record SubBudgetForwardApplyResponseDto(
        List<SubBudgetForwardMonthResultDto> months
) {
}
