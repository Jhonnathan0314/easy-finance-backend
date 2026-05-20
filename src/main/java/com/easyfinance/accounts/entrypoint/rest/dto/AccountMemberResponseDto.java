package com.easyfinance.accounts.entrypoint.rest.dto;

import java.time.Instant;

public record AccountMemberResponseDto(
        Long participantId,
        String email,
        String displayName,
        String role,
        String status,
        Instant joinedAt
) {
}
