package com.easyfinance.identity.application.command;

public record RegisterUserCommand(String email, String password, String fullName) {
}

