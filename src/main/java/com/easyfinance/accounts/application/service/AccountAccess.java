package com.easyfinance.accounts.application.service;

import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;

public record AccountAccess(Account account, AccountParticipant membership) {
}
