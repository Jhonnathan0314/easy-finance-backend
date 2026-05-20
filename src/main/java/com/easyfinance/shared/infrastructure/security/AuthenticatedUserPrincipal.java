package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.shared.application.CurrentUser;

import java.util.Set;

public record AuthenticatedUserPrincipal(
        Long userId,
        Long participantId,
        String email,
        Set<String> globalRoles
) {

    public CurrentUser toCurrentUser() {
        return new CurrentUser(userId, participantId, email, globalRoles, true);
    }
}

