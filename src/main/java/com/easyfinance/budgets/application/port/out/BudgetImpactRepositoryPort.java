package com.easyfinance.budgets.application.port.out;

import com.easyfinance.budgets.domain.model.BudgetImpact;

import java.util.List;
import java.util.Optional;

public interface BudgetImpactRepositoryPort {
    BudgetImpact save(BudgetImpact impact);

    Optional<BudgetImpact> findByAccountIdAndDebtIdAndPeriod(Long accountId, Long debtId, Integer year, Integer month);

    List<BudgetImpact> findByAccountIdAndBudgetId(Long accountId, Long budgetId);

    List<BudgetImpact> findActiveByAccountIdAndDebtIdOrderByPeriod(Long accountId, Long debtId);
}
