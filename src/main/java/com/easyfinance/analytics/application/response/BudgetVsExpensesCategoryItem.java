package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record BudgetVsExpensesCategoryItem(
        Long categoryId,
        String categoryName,
        BigDecimal budgetedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal executionPercentage
) {
}
