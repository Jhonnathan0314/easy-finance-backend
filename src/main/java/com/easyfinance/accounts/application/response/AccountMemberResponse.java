package com.easyfinance.accounts.application.response;

import java.time.Instant;

public record AccountMemberResponse(
        Long participantId,
        String email,
        String displayName,
        String role,
        String status,
        Instant joinedAt
) {
}
