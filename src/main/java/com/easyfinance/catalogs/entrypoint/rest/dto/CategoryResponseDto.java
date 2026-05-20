package com.easyfinance.catalogs.entrypoint.rest.dto;

import java.time.Instant;

public record CategoryResponseDto(
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
