package com.easyfinance.accounts.infrastructure.persistence;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.infrastructure.mapper.AccountParticipantPersistenceMapper;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantRoleJpa;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantStatusJpa;
import com.easyfinance.accounts.infrastructure.persistence.jpa.SpringDataAccountParticipantRepository;
import com.easyfinance.accounts.infrastructure.persistence.jpa.SpringDataAccountRepository;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaAccountParticipantRepositoryAdapter implements AccountParticipantRepositoryPort {

    private static final String UNIQUE_ACCOUNT_PARTICIPANT_CONSTRAINT = "uq_account_participants_account_participant";

    private final SpringDataAccountParticipantRepository accountParticipantRepository;
    private final SpringDataAccountRepository accountRepository;
    private final AccountParticipantPersistenceMapper mapper = new AccountParticipantPersistenceMapper();

    public JpaAccountParticipantRepositoryAdapter(
            SpringDataAccountParticipantRepository accountParticipantRepository,
            SpringDataAccountRepository accountRepository
    ) {
        this.accountParticipantRepository = accountParticipantRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountParticipant save(AccountParticipant accountParticipant) {
        AccountJpaEntity accountEntity = accountRepository.findById(accountParticipant.accountId())
                .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "Account was not found."));
        var entity = accountParticipant.id() == null
                ? mapper.toEntity(accountParticipant, accountEntity)
                : accountParticipantRepository.findById(accountParticipant.id())
                .orElseThrow(() -> new NotFoundException("ACCOUNT_MEMBER_NOT_FOUND", "Account member was not found."));
        if (accountParticipant.id() != null) {
            mapper.copyToEntity(accountParticipant, accountEntity, entity);
        }
        try {
            var saved = accountParticipantRepository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueAccountParticipantConstraint(ex)) {
                throw new BusinessRuleViolationException("ACCOUNT_MEMBER_ALREADY_EXISTS", "Account member already exists.", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<AccountParticipant> findByAccountIdAndParticipantId(Long accountId, Long participantId) {
        return accountParticipantRepository.findByAccountIdAndParticipantId(accountId, participantId).map(mapper::toDomain);
    }

    @Override
    public List<AccountParticipant> findByAccountId(Long accountId) {
        return accountParticipantRepository.findByAccountId(accountId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void lockByAccountId(Long accountId) {
        accountParticipantRepository.lockByAccountId(accountId);
    }

    @Override
    public long countByAccountIdAndRoleAndStatus(Long accountId, AccountParticipantRole role, AccountParticipantStatus status) {
        return accountParticipantRepository.countByAccountIdAndRoleAndStatus(
                accountId,
                AccountParticipantRoleJpa.valueOf(role.name()),
                AccountParticipantStatusJpa.valueOf(status.name())
        );
    }

    @Override
    public PageResponse<AccountParticipant> findMembershipsForParticipant(Long participantId, PageQuery pageQuery) {
        var page = accountParticipantRepository.findMembershipsForParticipant(
                participantId,
                PageRequest.of(pageQuery.page(), pageQuery.size())
        );
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static boolean isUniqueAccountParticipantConstraint(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && UNIQUE_ACCOUNT_PARTICIPANT_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(UNIQUE_ACCOUNT_PARTICIPANT_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
