package com.easyfinance.catalogs.application.response;

import java.time.Instant;

public record PaymentMethodResponse(
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
