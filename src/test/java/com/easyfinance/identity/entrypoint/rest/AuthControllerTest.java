package com.easyfinance.identity.entrypoint.rest;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.in.LogoutPort;
import com.easyfinance.identity.application.port.in.RefreshSessionPort;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.port.in.UpdateProfilePort;
import com.easyfinance.identity.application.response.AuthSessionResult;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import com.easyfinance.shared.infrastructure.security.RefreshTokenProperties;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final RegisterUserPort registerUserPort = mock(RegisterUserPort.class);
    private final LoginPort loginPort = mock(LoginPort.class);
    private final GetCurrentUserPort getCurrentUserPort = mock(GetCurrentUserPort.class);
    private final UpdateProfilePort updateProfilePort = mock(UpdateProfilePort.class);
    private final RefreshSessionPort refreshSessionPort = mock(RefreshSessionPort.class);
    private final LogoutPort logoutPort = mock(LogoutPort.class);
    private final RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(Duration.ofDays(30), true);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(registerUserPort, loginPort, getCurrentUserPort, updateProfilePort, refreshSessionPort, logoutPort, refreshTokenProperties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturnsCreatedTokenAndUserAndSetsRefreshCookie() throws Exception {
        when(registerUserPort.register(any())).thenReturn(sessionResult());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("jane@example.com", "abc12345", "Jane Doe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.participantId").value(10))
                .andExpect(jsonPath("$.user.globalRoles[*]", containsInAnyOrder("USER")))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=raw-refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    void loginReturnsTokenAndUserAndSetsRefreshCookie() throws Exception {
        when(loginPort.login(any())).thenReturn(sessionResult());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"abc12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=raw-refresh-token")));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(getCurrentUserPort.getCurrentUser()).thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.participantId").value(10));
    }

    @Test
    void updateProfileReturnsUpdatedUser() throws Exception {
        when(updateProfilePort.updateProfile(any())).thenReturn(new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Smith", Set.of("USER")));

        mockMvc.perform(put("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Jane Smith"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.participantId").value(10));
    }

    @Test
    void updateProfileValidationErrorsUseStandardFormat() throws Exception {
        mockMvc.perform(put("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registerValidationErrorsUseStandardFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","password":"short","fullName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void refreshDelegatesCookieAndReturnsNewAccessToken() throws Exception {
        when(refreshSessionPort.refreshSession("old-raw-refresh-token")).thenReturn(sessionResult());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-raw-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=raw-refresh-token")));
    }

    @Test
    void refreshWithoutCookiePropagatesFailure() throws Exception {
        when(refreshSessionPort.refreshSession(null)).thenThrow(new UnauthorizedOperationException("INVALID_REFRESH_TOKEN", "Refresh token is required."));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "raw-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(logoutPort).logout("raw-refresh-token");
    }

    @Test
    void logoutWithoutCookieStillSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

        verify(logoutPort).logout(eq(null));
    }

    private static AuthSessionResult sessionResult() {
        return new AuthSessionResult(tokenResponse(), "raw-refresh-token", Instant.now().plusSeconds(2592000));
    }

    private static AuthTokenResponse tokenResponse() {
        return new AuthTokenResponse("token", "Bearer", 3600L, userResponse());
    }

    private static AuthenticatedUserResponse userResponse() {
        return new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Doe", Set.of("USER"));
    }

    private record RequestBody(String email, String password, String fullName) {
    }
}
