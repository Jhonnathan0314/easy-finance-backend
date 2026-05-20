package com.easyfinance.income.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

final class IncomeText {

    private IncomeText() {
    }

    static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("INCOME_DESCRIPTION_REQUIRED", "Income description is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new BusinessRuleViolationException("INCOME_DESCRIPTION_INVALID", "Income description is too long.");
        }
        return trimmed;
    }
}
