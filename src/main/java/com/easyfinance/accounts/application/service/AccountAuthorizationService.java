package com.easyfinance.accounts.application.service;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountAuthorizationService {

    private final AccountRepositoryPort accountRepository;
    private final AccountParticipantRepositoryPort accountParticipantRepository;

    public AccountAuthorizationService(
            AccountRepositoryPort accountRepository,
            AccountParticipantRepositoryPort accountParticipantRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountParticipantRepository = accountParticipantRepository;
    }

    public AccountAccess requireActiveMember(Long accountId, Long participantId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountAuthorizationService::accountNotFound);
        AccountParticipant membership = accountParticipantRepository.findByAccountIdAndParticipantId(accountId, participantId)
                .filter(member -> member.status() == AccountParticipantStatus.ACTIVE)
                .orElseThrow(AccountAuthorizationService::accountNotFound);
        return new AccountAccess(account, membership);
    }

    public AccountAccess requireActiveAdminForActiveAccount(Long accountId, Long participantId) {
        AccountAccess access = requireActiveMember(accountId, participantId);
        try {
            access.account().ensureActive();
        } catch (BusinessRuleViolationException ex) {
            throw new ForbiddenOperationException("ACCOUNT_NOT_ACTIVE", "Account is not active.", ex);
        }
        if (access.membership().role() != AccountParticipantRole.ACCOUNT_ADMIN) {
            throw new ForbiddenOperationException("ACCOUNT_ADMIN_REQUIRED", "Account admin role is required.");
        }
        return access;
    }

    public AccountAccess requireActiveMemberForActiveAccount(Long accountId, Long participantId) {
        AccountAccess access = requireActiveMember(accountId, participantId);
        try {
            access.account().ensureActive();
        } catch (BusinessRuleViolationException ex) {
            throw new ForbiddenOperationException("ACCOUNT_NOT_ACTIVE", "Account is not active.", ex);
        }
        return access;
    }

    private static NotFoundException accountNotFound() {
        return new NotFoundException("ACCOUNT_NOT_FOUND", "Account was not found.");
    }
}
