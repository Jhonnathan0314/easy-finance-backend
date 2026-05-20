package com.easyfinance.imports.entrypoint.rest.dto;

public record ImportRowErrorDto(String column, String code, String message) {
}
