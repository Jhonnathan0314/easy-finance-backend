package com.easyfinance.debts.infrastructure.persistence;

import org.springframework.data.domain.Sort;

import java.util.Map;

final class DebtPaymentSort {

    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "paymentDate", "paymentDate",
            "amount", "amount",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private DebtPaymentSort() {
    }

    static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "paymentDate").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",", 2);
        String field = ALLOWED_FIELDS.getOrDefault(parts[0].trim(), "paymentDate");
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
