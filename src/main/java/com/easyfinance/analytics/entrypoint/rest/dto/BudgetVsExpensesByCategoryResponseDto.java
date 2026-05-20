package com.easyfinance.analytics.entrypoint.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record BudgetVsExpensesByCategoryResponseDto(
        Long accountId,
        Integer year,
        Integer month,
        LocalDate from,
        LocalDate to,
        List<BudgetVsExpensesCategoryItemDto> items
) {
}
