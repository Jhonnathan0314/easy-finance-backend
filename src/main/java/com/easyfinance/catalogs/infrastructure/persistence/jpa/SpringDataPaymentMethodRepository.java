package com.easyfinance.catalogs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SpringDataPaymentMethodRepository extends JpaRepository<PaymentMethodJpaEntity, Long>, JpaSpecificationExecutor<PaymentMethodJpaEntity> {

    Optional<PaymentMethodJpaEntity> findByAccountIdAndId(Long accountId, Long id);

    Optional<PaymentMethodJpaEntity> findByAccountIdAndNormalizedName(Long accountId, String normalizedName);

    List<PaymentMethodJpaEntity> findByAccountIdAndStatusOrderByNameAsc(Long accountId, CatalogStatusJpa status);

    boolean existsByAccountIdAndNormalizedNameAndStatus(Long accountId, String normalizedName, CatalogStatusJpa status);

    boolean existsByAccountIdAndNormalizedNameAndStatusAndIdNot(Long accountId, String normalizedName, CatalogStatusJpa status, Long id);
}
