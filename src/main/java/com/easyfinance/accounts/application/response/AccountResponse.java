package com.easyfinance.accounts.application.response;

import java.time.Instant;

public record AccountResponse(
        Long id,
        String name,
        String description,
        String status,
        String currentUserRole,
        Instant createdAt,
        Instant updatedAt
) {
}
