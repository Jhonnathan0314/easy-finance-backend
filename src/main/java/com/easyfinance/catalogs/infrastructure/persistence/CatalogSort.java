package com.easyfinance.catalogs.infrastructure.persistence;

import org.springframework.data.domain.Sort;

import java.util.Map;

final class CatalogSort {

    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "name", "name",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    private CatalogSort() {
    }

    static Sort from(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        String[] parts = sort.split(",", 2);
        String field = ALLOWED_FIELDS.getOrDefault(parts[0].trim(), "name");
        Sort.Direction direction = parts.length == 2 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
