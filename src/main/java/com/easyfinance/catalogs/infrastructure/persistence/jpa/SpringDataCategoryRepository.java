package com.easyfinance.catalogs.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, Long>, JpaSpecificationExecutor<CategoryJpaEntity> {

    Optional<CategoryJpaEntity> findByAccountIdAndId(Long accountId, Long id);

    List<CategoryJpaEntity> findByAccountIdAndNormalizedName(Long accountId, String normalizedName);

    Optional<CategoryJpaEntity> findByAccountIdAndTypeAndNormalizedName(Long accountId, CategoryTypeJpa type, String normalizedName);

    List<CategoryJpaEntity> findByAccountIdAndTypeAndStatusOrderByNameAsc(Long accountId, CategoryTypeJpa type, CatalogStatusJpa status);

    boolean existsByAccountIdAndTypeAndNormalizedNameAndStatus(Long accountId, CategoryTypeJpa type, String normalizedName, CatalogStatusJpa status);

    boolean existsByAccountIdAndTypeAndNormalizedNameAndStatusAndIdNot(
            Long accountId,
            CategoryTypeJpa type,
            String normalizedName,
            CatalogStatusJpa status,
            Long id
    );
}
