package com.easyfinance.shared.domain;

public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }

    public BusinessRuleViolationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

