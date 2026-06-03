package com.easyfinance.income.infrastructure.persistence;

import com.easyfinance.income.application.port.out.IncomeRepositoryPort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.application.response.PageResponse;
import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.income.infrastructure.mapper.IncomePersistenceMapper;
import com.easyfinance.income.infrastructure.persistence.jpa.IncomeJpaEntity;
import com.easyfinance.income.infrastructure.persistence.jpa.IncomeStatusJpa;
import com.easyfinance.income.infrastructure.persistence.jpa.SpringDataIncomeRepository;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.time.YearMonth;
import java.util.Optional;

@Repository
public class JpaIncomeRepositoryAdapter implements IncomeRepositoryPort {

    private final SpringDataIncomeRepository repository;
    private final IncomePersistenceMapper mapper = new IncomePersistenceMapper();

    public JpaIncomeRepositoryAdapter(SpringDataIncomeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Income save(Income income) {
        IncomeJpaEntity entity = income.id() == null
                ? mapper.toEntity(income)
                : repository.findByAccountIdAndId(income.accountId(), income.id())
                .orElseThrow(() -> new NotFoundException("INCOME_NOT_FOUND", "Income was not found."));
        if (income.id() != null) {
            mapper.copyToEntity(income, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Income> findByAccountIdAndId(Long accountId, Long incomeId) {
        return repository.findByAccountIdAndId(accountId, incomeId).map(mapper::toDomain);
    }

    @Override
    public PageResponse<Income> findAll(ListIncomesQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), IncomeSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<IncomeJpaEntity> specification(ListIncomesQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            IncomeStatus status = query.status() == null ? IncomeStatus.ACTIVE : query.status();
            predicate = builder.and(predicate, builder.equal(root.get("status"), IncomeStatusJpa.valueOf(status.name())));
            if (query.from() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("incomeDate"), query.from()));
            }
            if (query.to() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("incomeDate"), query.to()));
            }
            if (query.year() != null) {
                if (query.month() == null) {
                    YearMonth yearMonth = YearMonth.of(query.year(), 1);
                    predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("incomeDate"), yearMonth.atDay(1)));
                    predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("incomeDate"), YearMonth.of(query.year(), 12).atEndOfMonth()));
                } else {
                    YearMonth yearMonth = YearMonth.of(query.year(), query.month());
                    predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("incomeDate"), yearMonth.atDay(1)));
                    predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("incomeDate"), yearMonth.atEndOfMonth()));
                }
            }
            if (query.categoryId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("categoryId"), query.categoryId()));
            }
            if (query.participantId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("participantId"), query.participantId()));
            }
            if (query.search() != null) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("description")), likePattern(query.search()), '\\'));
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
