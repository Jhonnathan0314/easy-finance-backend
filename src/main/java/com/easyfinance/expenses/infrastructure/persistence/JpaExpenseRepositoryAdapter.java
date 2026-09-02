package com.easyfinance.expenses.infrastructure.persistence;

import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.infrastructure.mapper.ExpensePersistenceMapper;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseJpaEntity;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpensePaymentStateJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseSourceTypeJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseStatusJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseTypeJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.SpringDataExpenseRepository;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class JpaExpenseRepositoryAdapter implements ExpenseRepositoryPort {

    private final SpringDataExpenseRepository repository;
    private final ExpensePersistenceMapper mapper = new ExpensePersistenceMapper();

    public JpaExpenseRepositoryAdapter(SpringDataExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseJpaEntity entity = expense.id() == null
                ? mapper.toEntity(expense)
                : repository.findByAccountIdAndId(expense.accountId(), expense.id())
                .orElseThrow(() -> new NotFoundException("EXPENSE_NOT_FOUND", "Expense was not found."));
        if (expense.id() != null) {
            mapper.copyToEntity(expense, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Expense> findByAccountIdAndId(Long accountId, Long expenseId) {
        return repository.findByAccountIdAndId(accountId, expenseId).map(mapper::toDomain);
    }

    @Override
    public PageResponse<Expense> findAll(ListExpensesQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), ExpenseSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<ExpenseJpaEntity> specification(ListExpensesQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            ExpenseStatus status = query.status() == null ? ExpenseStatus.ACTIVE : query.status();
            predicate = builder.and(predicate, builder.equal(root.get("status"), ExpenseStatusJpa.valueOf(status.name())));
            if (query.from() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("expenseDate"), query.from()));
            }
            if (query.to() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("expenseDate"), query.to()));
            }
            if (query.categoryId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("categoryId"), query.categoryId()));
            }
            if (query.paymentMethodId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("paymentMethodId"), query.paymentMethodId()));
            }
            if (query.participantId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("participantId"), query.participantId()));
            }
            if (query.paymentState() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("paymentState"), ExpensePaymentStateJpa.valueOf(query.paymentState().name())));
            }
            if (query.expenseType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("expenseType"), ExpenseTypeJpa.valueOf(query.expenseType().name())));
            }
            if (query.search() != null) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("description")), likePattern(query.search()), '\\'));
            }
            if (query.debtPaymentOrigin() != null) {
                predicate = query.debtPaymentOrigin()
                        ? builder.and(predicate, builder.equal(root.get("sourceType"), ExpenseSourceTypeJpa.DEBT_PAYMENT))
                        : builder.and(predicate, builder.notEqual(root.get("sourceType"), ExpenseSourceTypeJpa.DEBT_PAYMENT));
            }
            return predicate;
        };
    }

    private static String likePattern(String search) {
        return "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
