package com.easyfinance.shared.infrastructure.security;

import com.easyfinance.shared.infrastructure.error.ApiErrorResponse;
import com.easyfinance.shared.infrastructure.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class RestSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        if (authException instanceof JwtAuthenticationException jwtException) {
            write(response, request, HttpStatus.UNAUTHORIZED, jwtException.code(), jwtException.getMessage());
            return;
        }
        write(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        if (accessDeniedException instanceof JwtAccessDeniedException jwtException) {
            write(response, request, HttpStatus.FORBIDDEN, jwtException.code(), jwtException.getMessage());
            return;
        }
        write(response, request, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.");
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
                List.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
