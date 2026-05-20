package com.easyfinance.budgets.application.query;

import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.shared.application.PageQuery;

public record ListBudgetsQuery(
        Long accountId,
        Integer year,
        BudgetStatus status,
        String sort,
        PageQuery pageQuery
) {
}
