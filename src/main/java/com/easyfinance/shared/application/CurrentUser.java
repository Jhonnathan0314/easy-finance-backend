package com.easyfinance.shared.application;

import java.util.Set;

public record CurrentUser(
        Long userId,
        Long participantId,
        String email,
        Set<String> globalRoles,
        boolean authenticated
) {

    public CurrentUser {
        globalRoles = globalRoles == null ? Set.of() : Set.copyOf(globalRoles);
    }

    public boolean hasGlobalRole(String role) {
        return globalRoles.contains(role);
    }
}
