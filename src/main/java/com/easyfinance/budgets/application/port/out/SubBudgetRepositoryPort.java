package com.easyfinance.budgets.application.port.out;

import com.easyfinance.budgets.domain.model.SubBudget;

import java.util.List;
import java.util.Optional;

public interface SubBudgetRepositoryPort {
    SubBudget save(SubBudget subBudget);

    Optional<SubBudget> findByAccountIdAndBudgetIdAndId(Long accountId, Long budgetId, Long subBudgetId);

    Optional<SubBudget> findDebtDerivedByAccountIdAndBudgetIdAndDebtId(Long accountId, Long budgetId, Long debtId);

    List<SubBudget> findByAccountIdAndBudgetId(Long accountId, Long budgetId);
}
