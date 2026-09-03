package com.easyfinance.identity.entrypoint.rest;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.in.LogoutPort;
import com.easyfinance.identity.application.port.in.RefreshSessionPort;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.port.in.UpdateProfilePort;
import com.easyfinance.identity.application.response.AuthSessionResult;
import com.easyfinance.identity.entrypoint.rest.dto.AuthTokenResponseDto;
import com.easyfinance.identity.entrypoint.rest.dto.AuthenticatedUserDto;
import com.easyfinance.identity.entrypoint.rest.dto.LoginRequest;
import com.easyfinance.identity.entrypoint.rest.dto.RegisterRequest;
import com.easyfinance.identity.entrypoint.rest.dto.UpdateProfileRequest;
import com.easyfinance.identity.entrypoint.rest.mapper.AuthRestMapper;
import com.easyfinance.shared.infrastructure.security.RefreshTokenProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    private final RegisterUserPort registerUserPort;
    private final LoginPort loginPort;
    private final GetCurrentUserPort getCurrentUserPort;
    private final UpdateProfilePort updateProfilePort;
    private final RefreshSessionPort refreshSessionPort;
    private final LogoutPort logoutPort;
    private final RefreshTokenProperties refreshTokenProperties;

    public AuthController(
            RegisterUserPort registerUserPort,
            LoginPort loginPort,
            GetCurrentUserPort getCurrentUserPort,
            UpdateProfilePort updateProfilePort,
            RefreshSessionPort refreshSessionPort,
            LogoutPort logoutPort,
            RefreshTokenProperties refreshTokenProperties
    ) {
        this.registerUserPort = registerUserPort;
        this.loginPort = loginPort;
        this.getCurrentUserPort = getCurrentUserPort;
        this.updateProfilePort = updateProfilePort;
        this.refreshSessionPort = refreshSessionPort;
        this.logoutPort = logoutPort;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokenResponseDto register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthSessionResult result = registerUserPort.register(AuthRestMapper.toCommand(request));
        setRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return AuthRestMapper.toDto(result.tokenResponse());
    }

    @PostMapping("/login")
    public AuthTokenResponseDto login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthSessionResult result = loginPort.login(AuthRestMapper.toCommand(request));
        setRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return AuthRestMapper.toDto(result.tokenResponse());
    }

    @PostMapping("/refresh")
    public AuthTokenResponseDto refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthSessionResult result = refreshSessionPort.refreshSession(refreshToken);
        setRefreshTokenCookie(response, result.refreshToken(), result.refreshTokenExpiresAt());
        return AuthRestMapper.toDto(result.tokenResponse());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        logoutPort.logout(refreshToken);
        clearRefreshTokenCookie(response);
    }

    @GetMapping("/me")
    public AuthenticatedUserDto me() {
        return AuthRestMapper.toDto(getCurrentUserPort.getCurrentUser());
    }

    @PutMapping("/me")
    public AuthenticatedUserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return AuthRestMapper.toDto(updateProfilePort.updateProfile(AuthRestMapper.toCommand(request)));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .sameSite("None")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshTokenProperties.cookieSecure())
                .sameSite("None")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
