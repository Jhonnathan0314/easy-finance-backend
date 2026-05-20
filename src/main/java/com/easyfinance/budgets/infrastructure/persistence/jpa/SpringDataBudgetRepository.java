package com.easyfinance.budgets.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataBudgetRepository extends JpaRepository<BudgetJpaEntity, Long>, JpaSpecificationExecutor<BudgetJpaEntity> {
    Optional<BudgetJpaEntity> findByAccountIdAndId(Long accountId, Long id);

    Optional<BudgetJpaEntity> findByAccountIdAndYearAndMonth(Long accountId, Integer year, Integer month);

    @Query(value = """
            INSERT INTO budgets (account_id, year, month, name, status)
            VALUES (:accountId, :year, :month, :name, 'ACTIVE')
            ON CONFLICT (account_id, year, month)
            DO UPDATE SET name = budgets.name
            RETURNING *
            """, nativeQuery = true)
    BudgetJpaEntity upsertMonthlyBudget(
            @Param("accountId") Long accountId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("name") String name
    );
}
