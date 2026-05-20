package com.easyfinance.catalogs.infrastructure.persistence;

import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.catalogs.infrastructure.mapper.PaymentMethodPersistenceMapper;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CatalogStatusJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodTypeJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.SpringDataPaymentMethodRepository;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class JpaPaymentMethodRepositoryAdapter implements PaymentMethodRepositoryPort {

    private static final String UNIQUE_ACTIVE_PAYMENT_METHOD = "uq_payment_methods_active_account_name";

    private final SpringDataPaymentMethodRepository repository;
    private final PaymentMethodPersistenceMapper mapper = new PaymentMethodPersistenceMapper();

    public JpaPaymentMethodRepositoryAdapter(SpringDataPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodJpaEntity entity = paymentMethod.id() == null
                ? mapper.toEntity(paymentMethod)
                : repository.findByAccountIdAndId(paymentMethod.accountId(), paymentMethod.id())
                .orElseThrow(() -> new NotFoundException("PAYMENT_METHOD_NOT_FOUND", "Payment method was not found."));
        if (paymentMethod.id() != null) {
            mapper.copyToEntity(paymentMethod, entity);
        }
        try {
            return mapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (isConstraint(ex, UNIQUE_ACTIVE_PAYMENT_METHOD)) {
                throw new BusinessRuleViolationException("PAYMENT_METHOD_ALREADY_EXISTS", "Payment method already exists.", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<PaymentMethod> findByAccountIdAndId(Long accountId, Long paymentMethodId) {
        return repository.findByAccountIdAndId(accountId, paymentMethodId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentMethod> findByAccountIdAndNormalizedName(Long accountId, String normalizedName) {
        return repository.findByAccountIdAndNormalizedName(accountId, normalizedName)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByAccountIdAndNormalizedName(Long accountId, String normalizedName) {
        return repository.existsByAccountIdAndNormalizedNameAndStatus(accountId, normalizedName, CatalogStatusJpa.ACTIVE);
    }

    @Override
    public boolean existsActiveByAccountIdAndNormalizedNameAndIdNot(Long accountId, String normalizedName, Long id) {
        return repository.existsByAccountIdAndNormalizedNameAndStatusAndIdNot(accountId, normalizedName, CatalogStatusJpa.ACTIVE, id);
    }

    @Override
    public PageResponse<PaymentMethod> findAll(ListPaymentMethodsQuery query) {
        var page = repository.findAll(specification(query), PageRequest.of(query.pageQuery().page(), query.pageQuery().size(), CatalogSort.from(query.sort())));
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public List<PaymentMethod> findActiveByAccountId(Long accountId) {
        return repository.findByAccountIdAndStatusOrderByNameAsc(accountId, CatalogStatusJpa.ACTIVE)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private static Specification<PaymentMethodJpaEntity> specification(ListPaymentMethodsQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            CatalogStatus status = query.status() == null ? CatalogStatus.ACTIVE : query.status();
            predicate = builder.and(predicate, builder.equal(root.get("status"), CatalogStatusJpa.valueOf(status.name())));
            if (query.type() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("type"), PaymentMethodTypeJpa.valueOf(query.type().name())));
            }
            if (query.search() != null) {
                String searchPattern = likePattern(query.search());
                var nameMatch = builder.like(root.get("normalizedName"), searchPattern, '\\');
                var descriptionMatch = builder.like(builder.lower(root.get("description")), searchPattern, '\\');
                predicate = builder.and(predicate, builder.or(nameMatch, descriptionMatch));
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
