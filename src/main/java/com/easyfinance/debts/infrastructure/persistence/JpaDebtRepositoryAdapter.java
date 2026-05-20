package com.easyfinance.debts.infrastructure.persistence;

import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.debts.infrastructure.mapper.DebtPersistenceMapper;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtSourceTypeJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtStateJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.SpringDataDebtRepository;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaDebtRepositoryAdapter implements DebtRepositoryPort {

    private final SpringDataDebtRepository repository;
    private final DebtPersistenceMapper mapper = new DebtPersistenceMapper();

    public JpaDebtRepositoryAdapter(SpringDataDebtRepository repository) {
        this.repository = repository;
    }

    @Override
    public Debt save(Debt debt) {
        DebtJpaEntity entity = debt.id() == null
                ? mapper.toEntity(debt)
                : repository.findByAccountIdAndId(debt.accountId(), debt.id())
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
        if (debt.id() != null) {
            mapper.copyToEntity(debt, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Debt> findByAccountIdAndId(Long accountId, Long debtId) {
        return repository.findByAccountIdAndId(accountId, debtId).map(mapper::toDomain);
    }

    @Override
    public Optional<Debt> findByAccountIdAndIdForUpdate(Long accountId, Long debtId) {
        return repository.findByAccountIdAndIdForUpdate(accountId, debtId).map(mapper::toDomain);
    }

    @Override
    public PageResponse<Debt> findAll(ListDebtsQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), DebtSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<DebtJpaEntity> specification(ListDebtsQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            DebtState state = query.state() == null ? DebtState.ACTIVE : query.state();
            predicate = builder.and(predicate, builder.equal(root.get("state"), DebtStateJpa.valueOf(state.name())));
            if (query.sourceType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("sourceType"), DebtSourceTypeJpa.valueOf(query.sourceType().name())));
            }
            if (query.participantId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("participantId"), query.participantId()));
            }
            if (query.from() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("startDate"), query.from()));
            }
            if (query.to() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("startDate"), query.to()));
            }
            return predicate;
        };
    }
}
