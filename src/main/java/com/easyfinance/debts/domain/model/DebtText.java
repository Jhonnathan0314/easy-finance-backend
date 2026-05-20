package com.easyfinance.debts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

final class DebtText {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_NOTES_LENGTH = 1000;

    private DebtText() {
    }

    static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("DEBT_NAME_REQUIRED", "Debt name is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleViolationException("DEBT_NAME_TOO_LONG", "Debt name is too long.");
        }
        return normalized;
    }

    static String normalizeDescription(String value) {
        return normalizeOptional(value, MAX_DESCRIPTION_LENGTH, "DEBT_DESCRIPTION_TOO_LONG", "Debt description is too long.");
    }

    static String normalizeNotes(String value) {
        return normalizeOptional(value, MAX_NOTES_LENGTH, "DEBT_NOTES_TOO_LONG", "Debt notes are too long.");
    }

    private static String normalizeOptional(String value, int maxLength, String code, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessRuleViolationException(code, message);
        }
        return normalized;
    }
}
