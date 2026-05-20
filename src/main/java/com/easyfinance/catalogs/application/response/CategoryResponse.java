package com.easyfinance.catalogs.application.response;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long accountId,
        String name,
        String description,
        String type,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
