package com.easyfinance.accounts.application.port.out;

import com.easyfinance.accounts.domain.model.Account;

import java.util.Optional;

public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(Long accountId);
}
