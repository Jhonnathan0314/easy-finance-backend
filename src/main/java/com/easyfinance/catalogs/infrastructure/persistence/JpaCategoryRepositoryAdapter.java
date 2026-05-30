package com.easyfinance.catalogs.infrastructure.persistence;

import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.infrastructure.mapper.CategoryPersistenceMapper;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CatalogStatusJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryTypeJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.SpringDataCategoryRepository;
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
public class JpaCategoryRepositoryAdapter implements CategoryRepositoryPort {

    private static final String UNIQUE_ACTIVE_CATEGORY = "uq_categories_active_account_type_name";

    private final SpringDataCategoryRepository repository;
    private final CategoryPersistenceMapper mapper = new CategoryPersistenceMapper();

    public JpaCategoryRepositoryAdapter(SpringDataCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = category.id() == null
                ? mapper.toEntity(category)
                : repository.findByAccountIdAndId(category.accountId(), category.id())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category was not found."));
        if (category.id() != null) {
            mapper.copyToEntity(category, entity);
        }
        try {
            return mapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (isConstraint(ex, UNIQUE_ACTIVE_CATEGORY)) {
                throw new BusinessRuleViolationException("CATEGORY_ALREADY_EXISTS", "Category already exists.", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Category> findByAccountIdAndId(Long accountId, Long categoryId) {
        return repository.findByAccountIdAndId(accountId, categoryId).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByAccountIdAndNormalizedName(Long accountId, String normalizedName) {
        return repository.findByAccountIdAndNormalizedName(accountId, normalizedName)
                .stream()
                .map(mapper::toDomain)
                .filter(category -> category.type() == CategoryType.EXPENSE)
                .findFirst()
                .or(() -> repository.findByAccountIdAndNormalizedName(accountId, normalizedName).stream().findFirst().map(mapper::toDomain));
    }

    @Override
    public Optional<Category> findByAccountIdAndTypeAndNormalizedName(Long accountId, CategoryType type, String normalizedName) {
        return repository.findByAccountIdAndTypeAndNormalizedName(accountId, CategoryTypeJpa.valueOf(type.name()), normalizedName)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByAccountIdAndTypeAndNormalizedName(Long accountId, CategoryType type, String normalizedName) {
        return repository.existsByAccountIdAndTypeAndNormalizedNameAndStatus(
                accountId,
                CategoryTypeJpa.valueOf(type.name()),
                normalizedName,
                CatalogStatusJpa.ACTIVE
        );
    }

    @Override
    public boolean existsActiveByAccountIdAndTypeAndNormalizedNameAndIdNot(Long accountId, CategoryType type, String normalizedName, Long id) {
        return repository.existsByAccountIdAndTypeAndNormalizedNameAndStatusAndIdNot(
                accountId,
                CategoryTypeJpa.valueOf(type.name()),
                normalizedName,
                CatalogStatusJpa.ACTIVE,
                id
        );
    }

    @Override
    public PageResponse<Category> findAll(ListCategoriesQuery query) {
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
    public List<Category> findActiveExpenseByAccountId(Long accountId) {
        return repository.findByAccountIdAndTypeAndStatusOrderByNameAsc(
                        accountId,
                        CategoryTypeJpa.EXPENSE,
                        CatalogStatusJpa.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Category> findActiveIncomeByAccountId(Long accountId) {
        return repository.findByAccountIdAndTypeAndStatusOrderByNameAsc(
                        accountId,
                        CategoryTypeJpa.INCOME,
                        CatalogStatusJpa.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private static Specification<CategoryJpaEntity> specification(ListCategoriesQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.equal(root.get("accountId"), query.accountId());
            CatalogStatus status = query.status() == null ? CatalogStatus.ACTIVE : query.status();
            predicate = builder.and(predicate, builder.equal(root.get("status"), CatalogStatusJpa.valueOf(status.name())));
            if (query.type() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("type"), CategoryTypeJpa.valueOf(query.type().name())));
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
