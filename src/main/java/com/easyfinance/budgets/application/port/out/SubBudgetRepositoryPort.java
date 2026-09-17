package com.easyfinance.budgets.application.port.out;

import com.easyfinance.budgets.domain.model.SubBudget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubBudgetRepositoryPort {
    SubBudget save(SubBudget subBudget);

    Optional<SubBudget> findByAccountIdAndBudgetIdAndId(Long accountId, Long budgetId, Long subBudgetId);

    Optional<SubBudget> findDebtDerivedByAccountIdAndBudgetIdAndDebtId(Long accountId, Long budgetId, Long debtId);

    List<SubBudget> findDebtDerivedActiveByAccountIdAndDebtId(Long accountId, Long debtId);

    List<SubBudget> findByAccountIdAndBudgetId(Long accountId, Long budgetId);

    Optional<SubBudget> findActiveByAccountIdAndBudgetIdAndRecurringGroupId(Long accountId, Long budgetId, UUID recurringGroupId);

    Optional<SubBudget> findManualActiveByAccountIdAndBudgetIdAndCategoryIdAndParticipantIdAndName(Long accountId, Long budgetId, Long categoryId, Long participantId, String name);
}
