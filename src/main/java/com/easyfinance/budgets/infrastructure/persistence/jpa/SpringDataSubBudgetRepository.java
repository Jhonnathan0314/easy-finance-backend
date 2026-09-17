package com.easyfinance.budgets.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSubBudgetRepository extends JpaRepository<SubBudgetJpaEntity, Long> {
    Optional<SubBudgetJpaEntity> findByAccountIdAndBudgetIdAndId(Long accountId, Long budgetId, Long id);

    Optional<SubBudgetJpaEntity> findByAccountIdAndBudgetIdAndSourceTypeAndDebtId(Long accountId, Long budgetId, SubBudgetSourceTypeJpa sourceType, Long debtId);

    List<SubBudgetJpaEntity> findByAccountIdAndDebtIdAndSourceTypeAndStatusOrderByBudgetIdAscIdAsc(
            Long accountId,
            Long debtId,
            SubBudgetSourceTypeJpa sourceType,
            SubBudgetStatusJpa status
    );

    List<SubBudgetJpaEntity> findByAccountIdAndBudgetIdOrderByIdAsc(Long accountId, Long budgetId);

    Optional<SubBudgetJpaEntity> findByAccountIdAndBudgetIdAndRecurringGroupIdAndStatus(
            Long accountId,
            Long budgetId,
            UUID recurringGroupId,
            SubBudgetStatusJpa status
    );

    Optional<SubBudgetJpaEntity> findByAccountIdAndBudgetIdAndSourceTypeAndStatusAndCategoryIdAndParticipantIdAndName(
            Long accountId,
            Long budgetId,
            SubBudgetSourceTypeJpa sourceType,
            SubBudgetStatusJpa status,
            Long categoryId,
            Long participantId,
            String name
    );
}
