package com.easyfinance.budgets.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SpringDataBudgetImpactRepository extends JpaRepository<BudgetImpactJpaEntity, Long> {
    Optional<BudgetImpactJpaEntity> findByAccountIdAndDebtIdAndPeriodYearAndPeriodMonth(Long accountId, Long debtId, Integer periodYear, Integer periodMonth);

    List<BudgetImpactJpaEntity> findByAccountIdAndBudgetIdOrderByPeriodYearAscPeriodMonthAscIdAsc(Long accountId, Long budgetId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BudgetImpactJpaEntity> findByAccountIdAndDebtIdAndStatusOrderByPeriodYearAscPeriodMonthAscIdAsc(Long accountId, Long debtId, BudgetImpactStatusJpa status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BudgetImpactJpaEntity> findByAccountIdAndDebtIdAndStatusInOrderByPeriodYearAscPeriodMonthAscIdAsc(Long accountId, Long debtId, List<BudgetImpactStatusJpa> statuses);
}
