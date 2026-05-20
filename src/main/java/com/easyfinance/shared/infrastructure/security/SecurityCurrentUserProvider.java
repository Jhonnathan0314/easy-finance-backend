package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return Optional.of(principal.toCurrentUser());
        }
        return Optional.empty();
    }
}

