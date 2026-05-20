package com.easyfinance.shared.domain;

public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }

    public NotFoundException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

