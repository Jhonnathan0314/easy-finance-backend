package com.easyfinance.identity.entrypoint.rest.dto;

import java.util.Set;

public record AuthenticatedUserDto(
        Long userId,
        Long participantId,
        String email,
        String fullName,
        Set<String> globalRoles
) {
}
