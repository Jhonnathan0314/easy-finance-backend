package com.easyfinance.accounts.entrypoint.rest.dto;

import java.time.Instant;

public record AccountResponseDto(
        Long id,
        String name,
        String description,
        String status,
        String currentUserRole,
        Instant createdAt,
        Instant updatedAt
) {
}
