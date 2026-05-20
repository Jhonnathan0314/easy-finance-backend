package com.easyfinance.expenses.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;

final class ExpenseText {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private ExpenseText() {
    }

    static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("EXPENSE_DESCRIPTION_REQUIRED", "Expense description is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleViolationException("EXPENSE_DESCRIPTION_TOO_LONG", "Expense description is too long.");
        }
        return normalized;
    }
}
