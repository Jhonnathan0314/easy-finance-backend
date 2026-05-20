package com.easyfinance.catalogs.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.Instant;
import java.util.Locale;

public final class PaymentMethod {

    private final Long id;
    private final Long accountId;
    private final String name;
    private final String normalizedName;
    private final String description;
    private final PaymentMethodType type;
    private final CatalogStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PaymentMethod(
            Long id,
            Long accountId,
            String name,
            String description,
            PaymentMethodType type,
            CatalogStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.accountId = requireId(accountId, "ACCOUNT_ID_REQUIRED", "Account id is required.");
        this.name = CatalogText.normalizeName(name, "PAYMENT_METHOD_NAME_REQUIRED", "PAYMENT_METHOD_NAME_TOO_LONG");
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
        this.description = CatalogText.normalizeDescription(description, "PAYMENT_METHOD_DESCRIPTION_TOO_LONG");
        this.type = requireType(type);
        this.status = requireStatus(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentMethod create(Long accountId, String name, String description, PaymentMethodType type) {
        return new PaymentMethod(null, accountId, name, description, type, CatalogStatus.ACTIVE, null, null);
    }

    public static PaymentMethod restore(
            Long id,
            Long accountId,
            String name,
            String description,
            PaymentMethodType type,
            CatalogStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new PaymentMethod(id, accountId, name, description, type, status, createdAt, updatedAt);
    }

    public PaymentMethod update(String name, String description) {
        ensureActive();
        return new PaymentMethod(id, accountId, name, description, type, status, createdAt, updatedAt);
    }

    public PaymentMethod deactivate() {
        ensureActive();
        return new PaymentMethod(id, accountId, name, description, type, CatalogStatus.INACTIVE, createdAt, updatedAt);
    }

    public void ensureActive() {
        if (status != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_INACTIVE", "Payment method is inactive.");
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

    public PaymentMethodType type() {
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

    private static PaymentMethodType requireType(PaymentMethodType value) {
        if (value == null) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_TYPE_REQUIRED", "Payment method type is required.");
        }
        return value;
    }

    private static CatalogStatus requireStatus(CatalogStatus value) {
        if (value == null) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_STATUS_REQUIRED", "Payment method status is required.");
        }
        return value;
    }
}
