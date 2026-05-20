package com.easyfinance.accounts.infrastructure.mapper;

import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountStatusJpa;

public class AccountPersistenceMapper {

    public Account toDomain(AccountJpaEntity entity) {
        return Account.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                AccountStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AccountJpaEntity toEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        copyToEntity(account, entity);
        return entity;
    }

    public void copyToEntity(Account account, AccountJpaEntity entity) {
        entity.setId(account.id());
        entity.setName(account.name());
        entity.setDescription(account.description());
        entity.setStatus(AccountStatusJpa.valueOf(account.status().name()));
    }
}
