package com.easyfinance.catalogs.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;
import java.util.Locale;

public final class Category {

    private final Long id;
    private final Long accountId;
    private final String name;
    private final String normalizedName;
    private final String description;
    private final CategoryType type;
    private final CatalogStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(
            Long id,
            Long accountId,
            String name,
            String description,
            CategoryType type,
            CatalogStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "ACCOUNT_ID_REQUIRED", "Account id is required.");
        this.name = CatalogText.normalizeName(name, "CATEGORY_NAME_REQUIRED", "CATEGORY_NAME_TOO_LONG");
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
        this.description = CatalogText.normalizeDescription(description, "CATEGORY_DESCRIPTION_TOO_LONG");
        this.type = requireType(type);
        this.status = requireStatus(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(Long accountId, String name, String description, CategoryType type) {
        return new Category(null, accountId, name, description, type, CatalogStatus.ACTIVE, null, null);
    }

    public static Category restore(
            Long id,
            Long accountId,
            String name,
            String description,
            CategoryType type,
            CatalogStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Category(id, accountId, name, description, type, status, createdAt, updatedAt);
    }

    public Category update(String name, String description) {
        ensureActive();
        return new Category(id, accountId, name, description, type, status, createdAt, updatedAt);
    }

    public Category deactivate() {
        ensureActive();
        return new Category(id, accountId, name, description, type, CatalogStatus.INACTIVE, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("CATEGORY_INACTIVE", "Category is inactive.");
        }
    }

    public Long id() {
        return id;
    }

    public Long accountId() {
        return accountId;
    }

    public String name() {
        return name;
    }

    public String normalizedName() {
        return normalizedName;
    }

    public String description() {
        return description;
    }

    public CategoryType type() {
        return type;
    }

    public CatalogStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Long requireId(Long value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
    }

    private static CategoryType requireType(CategoryType value) {
        if (value == null) {
            throw new BusinessRuleViolationException("CATEGORY_TYPE_REQUIRED", "Category type is required.");
        }
        return value;
    }

    private static CatalogStatus requireStatus(CatalogStatus value) {
        if (value == null) {
            throw new BusinessRuleViolationException("CATEGORY_STATUS_REQUIRED", "Category status is required.");
        }
        return value;
    }
}
