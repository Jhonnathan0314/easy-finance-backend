package com.easyfinance.analytics.application.response;

import java.time.LocalDate;
import java.util.List;

public record BudgetVsExpensesByCategoryResponse(
        Long accountId,
        Integer year,
        Integer month,
        LocalDate from,
        LocalDate to,
        List<BudgetVsExpensesCategoryItem> items
) {
}
