package com.easyfinance.budgets.infrastructure.persistence;

import com.easyfinance.budgets.application.port.out.SubBudgetRepositoryPort;
import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.budgets.infrastructure.mapper.SubBudgetPersistenceMapper;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SpringDataSubBudgetRepository;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetStatusJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SubBudgetSourceTypeJpa;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSubBudgetRepositoryAdapter implements SubBudgetRepositoryPort {

    private final SpringDataSubBudgetRepository repository;
    private final SubBudgetPersistenceMapper mapper = new SubBudgetPersistenceMapper();

    public JpaSubBudgetRepositoryAdapter(SpringDataSubBudgetRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubBudget save(SubBudget subBudget) {
        SubBudgetJpaEntity entity = subBudget.id() == null
                ? mapper.toEntity(subBudget)
                : repository.findByAccountIdAndBudgetIdAndId(subBudget.accountId(), subBudget.budgetId(), subBudget.id())
                .orElseThrow(() -> new NotFoundException("SUB_BUDGET_NOT_FOUND", "Sub-budget was not found."));
        if (subBudget.id() != null) {
            mapper.copyToEntity(subBudget, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<SubBudget> findByAccountIdAndBudgetIdAndId(Long accountId, Long budgetId, Long subBudgetId) {
        return repository.findByAccountIdAndBudgetIdAndId(accountId, budgetId, subBudgetId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubBudget> findDebtDerivedByAccountIdAndBudgetIdAndDebtId(Long accountId, Long budgetId, Long debtId) {
        return repository.findByAccountIdAndBudgetIdAndSourceTypeAndDebtId(accountId, budgetId, SubBudgetSourceTypeJpa.DEBT_DERIVED, debtId).map(mapper::toDomain);
    }

    @Override
    public List<SubBudget> findDebtDerivedActiveByAccountIdAndDebtId(Long accountId, Long debtId) {
        return repository.findByAccountIdAndDebtIdAndSourceTypeAndStatusOrderByBudgetIdAscIdAsc(
                        accountId,
                        debtId,
                        SubBudgetSourceTypeJpa.DEBT_DERIVED,
                        SubBudgetStatusJpa.ACTIVE
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<SubBudget> findByAccountIdAndBudgetId(Long accountId, Long budgetId) {
        return repository.findByAccountIdAndBudgetIdOrderByIdAsc(accountId, budgetId).stream().map(mapper::toDomain).toList();
    }
}
