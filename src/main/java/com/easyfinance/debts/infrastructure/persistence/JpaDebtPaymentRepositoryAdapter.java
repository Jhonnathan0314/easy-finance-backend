package com.easyfinance.debts.infrastructure.persistence;

import com.easyfinance.debts.application.port.out.DebtPaymentRepositoryPort;
import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.infrastructure.mapper.DebtPaymentPersistenceMapper;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentStatusJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentTypeJpa;
import com.easyfinance.debts.infrastructure.persistence.jpa.SpringDataDebtPaymentRepository;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaDebtPaymentRepositoryAdapter implements DebtPaymentRepositoryPort {

    private final SpringDataDebtPaymentRepository repository;
    private final DebtPaymentPersistenceMapper mapper = new DebtPaymentPersistenceMapper();

    public JpaDebtPaymentRepositoryAdapter(SpringDataDebtPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebtPayment save(DebtPayment payment) {
        DebtPaymentJpaEntity entity = payment.id() == null
                ? mapper.toEntity(payment)
                : repository.findByAccountIdAndDebtIdAndId(payment.accountId(), payment.debtId(), payment.id())
                .orElseThrow(() -> new NotFoundException("DEBT_PAYMENT_NOT_FOUND", "Debt payment was not found."));
        if (payment.id() != null) {
            mapper.copyToEntity(payment, entity);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<DebtPayment> findByAccountIdAndDebtIdAndId(Long accountId, Long debtId, Long paymentId) {
        return repository.findByAccountIdAndDebtIdAndId(accountId, debtId, paymentId).map(mapper::toDomain);
    }

    @Override
    public PageResponse<DebtPayment> findAll(ListDebtPaymentsQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), DebtPaymentSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Specification<DebtPaymentJpaEntity> specification(ListDebtPaymentsQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            predicate = builder.and(predicate, builder.equal(root.get("debtId"), query.debtId()));
            DebtPaymentStatus status = query.status() == null ? DebtPaymentStatus.ACTIVE : query.status();
            predicate = builder.and(predicate, builder.equal(root.get("status"), DebtPaymentStatusJpa.valueOf(status.name())));
            if (query.paymentType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("paymentType"), DebtPaymentTypeJpa.valueOf(query.paymentType().name())));
            }
            if (query.from() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("paymentDate"), query.from()));
            }
            if (query.to() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("paymentDate"), query.to()));
            }
            return predicate;
        };
    }
}
