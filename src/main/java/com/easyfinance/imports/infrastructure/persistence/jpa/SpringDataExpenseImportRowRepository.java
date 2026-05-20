package com.easyfinance.imports.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataExpenseImportRowRepository extends JpaRepository<ExpenseImportRowJpaEntity, Long> {

    List<ExpenseImportRowJpaEntity> findByAccountIdAndBatchIdOrderByRowNumberAsc(Long accountId, Long batchId);

    Optional<ExpenseImportRowJpaEntity> findByAccountIdAndId(Long accountId, Long id);
}
