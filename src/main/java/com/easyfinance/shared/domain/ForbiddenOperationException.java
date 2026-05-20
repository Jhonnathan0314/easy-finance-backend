package com.easyfinance.shared.domain;

public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }

    public ForbiddenOperationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

