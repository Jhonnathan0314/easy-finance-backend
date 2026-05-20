package com.easyfinance.shared.infrastructure.security;

import org.springframework.security.access.AccessDeniedException;

public class JwtAccessDeniedException extends AccessDeniedException {

    private final String code;

    public JwtAccessDeniedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
