package com.easyfinance.accounts.application.command;

public record UpdateAccountCommand(Long accountId, String name, String description) {
}
