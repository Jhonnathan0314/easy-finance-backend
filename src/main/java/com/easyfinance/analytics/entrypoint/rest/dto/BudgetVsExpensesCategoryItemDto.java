package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record BudgetVsExpensesCategoryItemDto(
        Long categoryId,
        String categoryName,
        BigDecimal budgetedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal executionPercentage
) {
}
