package com.easyfinance.accounts.application.usecase;

import com.easyfinance.accounts.application.command.AddAccountMemberCommand;
import com.easyfinance.accounts.application.command.ChangeAccountMemberRoleCommand;
import com.easyfinance.accounts.application.command.CreateAccountCommand;
import com.easyfinance.accounts.application.command.UpdateAccountCommand;
import com.easyfinance.accounts.application.port.in.AddAccountMemberPort;
import com.easyfinance.accounts.application.port.in.ArchiveAccountPort;
import com.easyfinance.accounts.application.port.in.ChangeAccountMemberRolePort;
import com.easyfinance.accounts.application.port.in.CreateAccountPort;
import com.easyfinance.accounts.application.port.in.GetAccountPort;
import com.easyfinance.accounts.application.port.in.ListAccountMembersPort;
import com.easyfinance.accounts.application.port.in.ListAccountsPort;
import com.easyfinance.accounts.application.port.in.RemoveAccountMemberPort;
import com.easyfinance.accounts.application.port.in.UpdateAccountPort;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.query.ListAccountsQuery;
import com.easyfinance.accounts.application.response.AccountMemberResponse;
import com.easyfinance.accounts.application.response.AccountResponse;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountManagementUseCase implements
        CreateAccountPort,
        ListAccountsPort,
        GetAccountPort,
        UpdateAccountPort,
        ArchiveAccountPort,
        ListAccountMembersPort,
        AddAccountMemberPort,
        ChangeAccountMemberRolePort,
        RemoveAccountMemberPort {

    private final CurrentUserProvider currentUserProvider;
    private final ParticipantLookupPort participantLookupPort;
    private final AccountRepositoryPort accountRepository;
    private final AccountParticipantRepositoryPort accountParticipantRepository;
    private final AccountAuthorizationService authorizationService;

    public AccountManagementUseCase(
            CurrentUserProvider currentUserProvider,
            ParticipantLookupPort participantLookupPort,
            AccountRepositoryPort accountRepository,
            AccountParticipantRepositoryPort accountParticipantRepository,
            AccountAuthorizationService authorizationService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.participantLookupPort = participantLookupPort;
        this.accountRepository = accountRepository;
        this.accountParticipantRepository = accountParticipantRepository;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountCommand command) {
        Long participantId = currentParticipantId();
        requireActiveParticipant(participantId);

        Account savedAccount = accountRepository.save(Account.create(command.name(), command.description()));
        AccountParticipant savedMembership = accountParticipantRepository.save(AccountParticipant.createAdmin(savedAccount.id(), participantId));

        return toAccountResponse(savedAccount, savedMembership.role());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> listMyAccounts(ListAccountsQuery query) {
        Long participantId = currentParticipantId();
        PageResponse<AccountParticipant> memberships = accountParticipantRepository.findMembershipsForParticipant(participantId, query.pageQuery());
        List<AccountResponse> content = memberships.content()
                .stream()
                .map(membership -> {
                    Account account = accountRepository.findById(membership.accountId())
                            .orElseThrow(() -> new NotFoundException("ACCOUNT_NOT_FOUND", "Account was not found."));
                    return toAccountResponse(account, membership.role());
                })
                .toList();
        return new PageResponse<>(content, memberships.page(), memberships.size(), memberships.totalElements(), memberships.totalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        AccountAccess access = authorizationService.requireActiveMember(accountId, currentParticipantId());
        return toAccountResponse(access.account(), access.membership().role());
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(UpdateAccountCommand command) {
        AccountAccess access = authorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Account saved = accountRepository.save(access.account().update(command.name(), command.description()));
        return toAccountResponse(saved, access.membership().role());
    }

    @Override
    @Transactional
    public AccountResponse archiveAccount(Long accountId) {
        AccountAccess access = authorizationService.requireActiveAdminForActiveAccount(accountId, currentParticipantId());
        Account saved = accountRepository.save(access.account().archive());
        return toAccountResponse(saved, access.membership().role());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountMemberResponse> listMembers(Long accountId) {
        AccountAccess access = authorizationService.requireActiveMember(accountId, currentParticipantId());
        List<AccountParticipant> memberships = accountParticipantRepository.findByAccountId(accountId);
        if (access.membership().role() != AccountParticipantRole.ACCOUNT_ADMIN) {
            memberships = memberships.stream()
                    .filter(membership -> membership.status() == AccountParticipantStatus.ACTIVE)
                    .toList();
        }
        Map<Long, ParticipantInfo> participants = participantLookupPort.findByParticipantIds(
                memberships.stream().map(AccountParticipant::participantId).collect(Collectors.toSet())
        );
        return memberships.stream()
                .map(membership -> toMemberResponse(membership, participants.get(membership.participantId())))
                .toList();
    }

    @Override
    @Transactional
    public AccountMemberResponse addMember(AddAccountMemberCommand command) {
        authorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        ParticipantInfo target = participantLookupPort.findActiveByEmail(normalizeEmail(command.email()))
                .orElseThrow(() -> new NotFoundException("ACCOUNT_MEMBER_NOT_FOUND", "Account member was not found."));

        AccountParticipant membership = accountParticipantRepository.findByAccountIdAndParticipantId(command.accountId(), target.participantId())
                .map(existing -> reactivateOrReject(existing, command.role()))
                .orElseGet(() -> AccountParticipant.create(command.accountId(), target.participantId(), command.role()));

        AccountParticipant saved = accountParticipantRepository.save(membership);
        return toMemberResponse(saved, target);
    }

    @Override
    @Transactional
    public AccountMemberResponse changeMemberRole(ChangeAccountMemberRoleCommand command) {
        accountParticipantRepository.lockByAccountId(command.accountId());
        authorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        AccountParticipant membership = accountParticipantRepository.findByAccountIdAndParticipantId(command.accountId(), command.participantId())
                .filter(member -> member.status() == AccountParticipantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_MEMBER_NOT_FOUND", "Account member was not found."));

        ensureNotRemovingLastAdmin(membership, command.role());
        AccountParticipant saved = accountParticipantRepository.save(membership.changeRole(command.role()));
        ParticipantInfo participant = participantLookupPort.findByParticipantId(saved.participantId()).orElse(null);
        return toMemberResponse(saved, participant);
    }

    @Override
    @Transactional
    public void removeMember(Long accountId, Long participantId) {
        accountParticipantRepository.lockByAccountId(accountId);
        authorizationService.requireActiveAdminForActiveAccount(accountId, currentParticipantId());
        AccountParticipant membership = accountParticipantRepository.findByAccountIdAndParticipantId(accountId, participantId)
                .filter(member -> member.status() == AccountParticipantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("ACCOUNT_MEMBER_NOT_FOUND", "Account member was not found."));

        ensureNotRemovingLastAdmin(membership, null);
        accountParticipantRepository.save(membership.deactivate());
    }

    private AccountParticipant reactivateOrReject(AccountParticipant existing, AccountParticipantRole role) {
        if (existing.status() == AccountParticipantStatus.ACTIVE) {
            throw new BusinessRuleViolationException("ACCOUNT_MEMBER_ALREADY_EXISTS", "Account member already exists.");
        }
        return existing.activate(role);
    }

    private void ensureNotRemovingLastAdmin(AccountParticipant membership, AccountParticipantRole newRole) {
        boolean removesAdmin = membership.isActiveAdmin() && newRole != AccountParticipantRole.ACCOUNT_ADMIN;
        if (!removesAdmin) {
            return;
        }
        long activeAdmins = accountParticipantRepository.countByAccountIdAndRoleAndStatus(
                membership.accountId(),
                AccountParticipantRole.ACCOUNT_ADMIN,
                AccountParticipantStatus.ACTIVE
        );
        if (activeAdmins <= 1) {
            throw new ForbiddenOperationException("ACCOUNT_LAST_ADMIN_REQUIRED", "Account must keep at least one active admin.");
        }
    }

    private Long currentParticipantId() {
        CurrentUser currentUser = currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
        return currentUser.participantId();
    }

    private void requireActiveParticipant(Long participantId) {
        participantLookupPort.findByParticipantId(participantId)
                .filter(ParticipantInfo::active)
                .orElseThrow(() -> new ForbiddenOperationException("PARTICIPANT_NOT_ACTIVE", "Participant is not active."));
    }

    private AccountResponse toAccountResponse(Account account, AccountParticipantRole currentUserRole) {
        return new AccountResponse(
                account.id(),
                account.name(),
                account.description(),
                account.status().name(),
                currentUserRole.name(),
                account.createdAt(),
                account.updatedAt()
        );
    }

    private AccountMemberResponse toMemberResponse(AccountParticipant membership, ParticipantInfo participant) {
        return new AccountMemberResponse(
                membership.participantId(),
                participant == null ? null : participant.email(),
                participant == null ? null : participant.displayName(),
                membership.role().name(),
                membership.status().name(),
                membership.joinedAt()
        );
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleViolationException("EMAIL_REQUIRED", "Email is required.");
        }
        return email.trim().toLowerCase();
    }
}
