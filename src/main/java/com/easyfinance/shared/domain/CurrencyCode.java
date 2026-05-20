package com.easyfinance.shared.domain;

import java.util.Locale;

public enum CurrencyCode {
    COP;

    public static CurrencyCode from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException("CURRENCY_REQUIRED", "Currency is required.");
        }

        try {
            return CurrencyCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("UNSUPPORTED_CURRENCY", "Only COP is supported in the MVP.");
        }
    }
}

