package com.easyfinance.imports.application.response;

public record ImportRowErrorResponse(String column, String code, String message) {
}
