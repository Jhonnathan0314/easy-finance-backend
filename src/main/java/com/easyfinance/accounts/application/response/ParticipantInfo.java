package com.easyfinance.accounts.application.response;

public record ParticipantInfo(
        Long participantId,
        Long userId,
        String email,
        String displayName,
        boolean active
) {
}
