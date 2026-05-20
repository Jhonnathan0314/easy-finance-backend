package com.easyfinance.shared.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.easyfinance.shared.application.CurrentUser;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void invalidBearerTokenReturnsUnauthorizedAndStopsChain() throws Exception {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        when(tokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));
        FilterChain chain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenService,
                new RestSecurityExceptionHandler(objectMapper()),
                AuthenticatedUserStatusValidator.noop()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"INVALID_TOKEN\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }

    @Test
    void activeBearerTokenValidatesCurrentIdentityAndContinuesChain() throws Exception {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        CurrentUser currentUser = new CurrentUser(1L, 2L, "jane@example.com", Set.of("USER"), true);
        when(tokenService.validate("good-token")).thenReturn(currentUser);
        AuthenticatedUserStatusValidator validator = mock(AuthenticatedUserStatusValidator.class);
        FilterChain chain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, new RestSecurityExceptionHandler(objectMapper()), validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(validator).validate(currentUser);
        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void inactiveAuthenticatedIdentityReturnsForbiddenAndStopsChain() throws Exception {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        CurrentUser currentUser = new CurrentUser(1L, 2L, "jane@example.com", Set.of("USER"), true);
        when(tokenService.validate("blocked-token")).thenReturn(currentUser);
        AuthenticatedUserStatusValidator validator = user -> {
            throw new JwtAccessDeniedException("USER_BLOCKED", "User is blocked.");
        };
        FilterChain chain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenService, new RestSecurityExceptionHandler(objectMapper()), validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        request.addHeader("Authorization", "Bearer blocked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"USER_BLOCKED\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
