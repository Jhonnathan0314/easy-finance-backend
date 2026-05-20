package com.easyfinance.income.infrastructure.persistence;

import org.springframework.data.domain.Sort;

import java.util.Map;

final class IncomeSort {

    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "incomeDate", "incomeDate",
            "amount", "amount",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private IncomeSort() {
    }

    static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "incomeDate").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",", 2);
        String field = ALLOWED_FIELDS.getOrDefault(parts[0].trim(), "incomeDate");
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
