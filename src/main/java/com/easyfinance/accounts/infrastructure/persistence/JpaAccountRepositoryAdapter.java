package com.easyfinance.accounts.infrastructure.persistence;

import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.infrastructure.mapper.AccountPersistenceMapper;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.SpringDataAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaAccountRepositoryAdapter implements AccountRepositoryPort {

    private final SpringDataAccountRepository accountRepository;
    private final AccountPersistenceMapper mapper = new AccountPersistenceMapper();

    public JpaAccountRepositoryAdapter(SpringDataAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = account.id() == null
                ? mapper.toEntity(account)
                : accountRepository.findById(account.id())
                .orElseThrow(() -> new com.easyfinance.shared.domain.NotFoundException("ACCOUNT_NOT_FOUND", "Account was not found."));
        if (account.id() != null) {
            mapper.copyToEntity(account, entity);
        }
        AccountJpaEntity saved = accountRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return accountRepository.findById(accountId).map(mapper::toDomain);
    }
}
