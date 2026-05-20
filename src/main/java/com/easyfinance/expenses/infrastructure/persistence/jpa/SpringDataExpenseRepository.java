package com.easyfinance.expenses.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SpringDataExpenseRepository extends JpaRepository<ExpenseJpaEntity, Long>, JpaSpecificationExecutor<ExpenseJpaEntity> {

    Optional<ExpenseJpaEntity> findByAccountIdAndId(Long accountId, Long id);
}
