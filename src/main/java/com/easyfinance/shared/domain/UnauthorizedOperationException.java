package com.easyfinance.shared.domain;

public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String code, String message) {
        super(code, message);
    }

    public UnauthorizedOperationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

