package com.easyfinance.catalogs.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

final class CatalogText {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private CatalogText() {
    }

    static String normalizeName(String value, String requiredCode, String tooLongCode) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(requiredCode, "Name is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleViolationException(tooLongCode, "Name is too long.");
        }
        return normalized;
    }

    static String normalizeDescription(String value, String tooLongCode) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleViolationException(tooLongCode, "Description is too long.");
        }
        return normalized;
    }
}
