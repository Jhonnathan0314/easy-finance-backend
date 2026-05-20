package com.easyfinance.shared.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final RestSecurityExceptionHandler restSecurityExceptionHandler;
    private final AuthenticatedUserStatusValidator authenticatedUserStatusValidator;

    @Autowired
    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            RestSecurityExceptionHandler restSecurityExceptionHandler,
            ObjectProvider<AuthenticatedUserStatusValidator> authenticatedUserStatusValidatorProvider
    ) {
        this(
                jwtTokenService,
                restSecurityExceptionHandler,
                authenticatedUserStatusValidatorProvider.getIfAvailable(AuthenticatedUserStatusValidator::noop)
        );
    }

    JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            RestSecurityExceptionHandler restSecurityExceptionHandler,
            AuthenticatedUserStatusValidator authenticatedUserStatusValidator
    ) {
        this.jwtTokenService = jwtTokenService;
        this.restSecurityExceptionHandler = restSecurityExceptionHandler;
        this.authenticatedUserStatusValidator = authenticatedUserStatusValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            try {
                var currentUser = jwtTokenService.validate(authorizationHeader.substring(BEARER_PREFIX.length()));
                authenticatedUserStatusValidator.validate(currentUser);
                var principal = new AuthenticatedUserPrincipal(
                        currentUser.userId(),
                        currentUser.participantId(),
                        currentUser.email(),
                        currentUser.globalRoles()
                );
                var authorities = currentUser.globalRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toUnmodifiableSet());
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtAuthenticationException ex) {
                SecurityContextHolder.clearContext();
                restSecurityExceptionHandler.commence(request, response, ex);
                return;
            } catch (JwtAccessDeniedException ex) {
                SecurityContextHolder.clearContext();
                restSecurityExceptionHandler.handle(request, response, ex);
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
