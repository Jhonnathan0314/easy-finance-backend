package com.easyfinance.budgets.infrastructure.persistence;

import org.springframework.data.domain.Sort;

public final class BudgetSort {

    private BudgetSort() {
    }

    public static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "year").and(Sort.by(Sort.Direction.DESC, "month"));
        }
        String[] parts = sort.split(",", 2);
        String property = switch (parts[0]) {
            case "year", "month", "status", "createdAt" -> parts[0];
            default -> "year";
        };
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
