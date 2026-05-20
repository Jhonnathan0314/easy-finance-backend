package com.easyfinance.budgets.infrastructure.persistence;

import com.easyfinance.budgets.application.port.out.BudgetImpactRepositoryPort;
import com.easyfinance.budgets.domain.model.BudgetImpact;
import com.easyfinance.budgets.infrastructure.mapper.BudgetImpactPersistenceMapper;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetImpactJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetImpactStatusJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SpringDataBudgetImpactRepository;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaBudgetImpactRepositoryAdapter implements BudgetImpactRepositoryPort {

    private final SpringDataBudgetImpactRepository repository;
    private final BudgetImpactPersistenceMapper mapper = new BudgetImpactPersistenceMapper();

    public JpaBudgetImpactRepositoryAdapter(SpringDataBudgetImpactRepository repository) {
        this.repository = repository;
    }

    @Override
    public BudgetImpact save(BudgetImpact impact) {
        BudgetImpactJpaEntity entity = impact.id() == null
                ? mapper.toEntity(impact)
                : repository.findById(impact.id())
                .filter(existing -> existing.getAccountId().equals(impact.accountId()))
                .orElseThrow(() -> new NotFoundException("BUDGET_IMPACT_NOT_FOUND", "Budget impact was not found."));
        if (impact.id() != null) {
            mapper.copyToEntity(impact, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<BudgetImpact> findByAccountIdAndDebtIdAndPeriod(Long accountId, Long debtId, Integer year, Integer month) {
        return repository.findByAccountIdAndDebtIdAndPeriodYearAndPeriodMonth(accountId, debtId, year, month).map(mapper::toDomain);
    }

    @Override
    public List<BudgetImpact> findByAccountIdAndBudgetId(Long accountId, Long budgetId) {
        return repository.findByAccountIdAndBudgetIdOrderByPeriodYearAscPeriodMonthAscIdAsc(accountId, budgetId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<BudgetImpact> findActiveByAccountIdAndDebtIdOrderByPeriod(Long accountId, Long debtId) {
        return repository.findByAccountIdAndDebtIdAndStatusOrderByPeriodYearAscPeriodMonthAscIdAsc(accountId, debtId, BudgetImpactStatusJpa.ACTIVE).stream().map(mapper::toDomain).toList();
    }
}
