package com.easyfinance.budgets.application.port.out;

import com.easyfinance.budgets.application.query.ListBudgetsQuery;
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.budgets.domain.model.Budget;

import java.util.Optional;

public interface BudgetRepositoryPort {
    Budget save(Budget budget);

    Optional<Budget> findByAccountIdAndId(Long accountId, Long budgetId);

    Optional<Budget> findByAccountIdAndYearAndMonth(Long accountId, Integer year, Integer month);

    Budget getOrCreateMonthlyBudget(Long accountId, Integer year, Integer month, String defaultName);

    PageResponse<Budget> findAll(ListBudgetsQuery query);
}
