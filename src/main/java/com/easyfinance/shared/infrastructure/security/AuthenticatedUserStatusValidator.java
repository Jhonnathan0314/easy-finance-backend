package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.shared.application.CurrentUser;

@FunctionalInterface
public interface AuthenticatedUserStatusValidator {

    void validate(CurrentUser currentUser);

    static AuthenticatedUserStatusValidator noop() {
        return currentUser -> {
        };
    }
}
