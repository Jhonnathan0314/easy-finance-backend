package com.easyfinance.expenses.infrastructure.persistence;

import org.springframework.data.domain.Sort;

import java.util.Map;

final class ExpenseSort {

    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "expenseDate", "expenseDate",
            "amount", "amount",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private ExpenseSort() {
    }

    static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",", 2);
        String field = ALLOWED_FIELDS.getOrDefault(parts[0].trim(), "expenseDate");
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
