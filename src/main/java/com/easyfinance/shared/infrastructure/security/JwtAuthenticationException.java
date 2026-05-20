package com.easyfinance.shared.infrastructure.security;

import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {

    private final String code;

    public JwtAuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

