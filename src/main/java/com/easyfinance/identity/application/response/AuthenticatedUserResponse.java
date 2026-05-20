package com.easyfinance.identity.application.response;

import java.util.Set;

public record AuthenticatedUserResponse(
        Long userId,
        Long participantId,
        String email,
        String fullName,
        Set<String> globalRoles
) {
}

