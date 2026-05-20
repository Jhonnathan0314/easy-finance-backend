package com.easyfinance.catalogs.infrastructure.mapper;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CatalogStatusJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryTypeJpa;

public class CategoryPersistenceMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        return Category.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getName(),
                entity.getDescription(),
                CategoryType.valueOf(entity.getType().name()),
                CatalogStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CategoryJpaEntity toEntity(Category category) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        copyToEntity(category, entity);
        return entity;
    }

    public void copyToEntity(Category category, CategoryJpaEntity entity) {
        entity.setId(category.id());
        entity.setAccountId(category.accountId());
        entity.setName(category.name());
        entity.setNormalizedName(category.normalizedName());
        entity.setDescription(category.description());
        entity.setType(CategoryTypeJpa.valueOf(category.type().name()));
        entity.setStatus(CatalogStatusJpa.valueOf(category.status().name()));
    }
}
