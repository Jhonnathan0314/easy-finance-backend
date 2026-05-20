package com.easyfinance.budgets.infrastructure.persistence;

import com.easyfinance.budgets.application.port.out.BudgetRepositoryPort;
import com.easyfinance.budgets.application.query.ListBudgetsQuery;
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.infrastructure.mapper.BudgetPersistenceMapper;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetStatusJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SpringDataBudgetRepository;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaBudgetRepositoryAdapter implements BudgetRepositoryPort {

    private static final String UNIQUE_ACCOUNT_YEAR_MONTH = "uq_budgets_account_year_month";

    private final SpringDataBudgetRepository repository;
    private final BudgetPersistenceMapper mapper = new BudgetPersistenceMapper();

    public JpaBudgetRepositoryAdapter(SpringDataBudgetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Budget save(Budget budget) {
        BudgetJpaEntity entity = budget.id() == null
                ? mapper.toEntity(budget)
                : repository.findByAccountIdAndId(budget.accountId(), budget.id())
                .orElseThrow(() -> new NotFoundException("BUDGET_NOT_FOUND", "Budget was not found."));
        if (budget.id() != null) {
            mapper.copyToEntity(budget, entity);
        }
        try {
            return mapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (isConstraint(ex, UNIQUE_ACCOUNT_YEAR_MONTH)) {
                throw new BusinessRuleViolationException("BUDGET_TARGET_ALREADY_EXISTS", "Target budget already exists.", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Budget> findByAccountIdAndId(Long accountId, Long budgetId) {
        return repository.findByAccountIdAndId(accountId, budgetId).map(mapper::toDomain);
    }

    @Override
    public Optional<Budget> findByAccountIdAndYearAndMonth(Long accountId, Integer year, Integer month) {
        return repository.findByAccountIdAndYearAndMonth(accountId, year, month).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Budget getOrCreateMonthlyBudget(Long accountId, Integer year, Integer month, String defaultName) {
        return mapper.toDomain(repository.upsertMonthlyBudget(accountId, year, month, defaultName));
    }

    @Override
    public PageResponse<Budget> findAll(ListBudgetsQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), BudgetSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<BudgetJpaEntity> specification(ListBudgetsQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            if (query.year() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("year"), query.year()));
            }
            BudgetStatus status = query.status();
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), BudgetStatusJpa.valueOf(status.name())));
            }
            return predicate;
        };
    }

    private static boolean isConstraint(DataIntegrityViolationException ex, String constraintName) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && constraintName.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
