package com.easyfinance.accounts.application.usecase;

import com.easyfinance.accounts.application.command.AddAccountMemberCommand;
import com.easyfinance.accounts.application.command.ChangeAccountMemberRoleCommand;
import com.easyfinance.accounts.application.command.CreateAccountCommand;
import com.easyfinance.accounts.application.command.UpdateAccountCommand;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.query.ListAccountsQuery;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final ParticipantLookupPort participantLookupPort = mock(ParticipantLookupPort.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort memberRepository = mock(AccountParticipantRepositoryPort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, memberRepository);
    private final AccountManagementUseCase useCase = new AccountManagementUseCase(
            currentUserProvider,
            participantLookupPort,
            accountRepository,
            memberRepository,
            authorizationService
    );

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "owner@example.com", Set.of("USER"), true)));
        when(participantLookupPort.findByParticipantId(10L)).thenReturn(Optional.of(new ParticipantInfo(10L, 1L, "owner@example.com", "Owner", true)));
    }

    @Test
    void createAccountCreatesAdminMembership() {
        when(accountRepository.save(any(Account.class))).thenReturn(account(AccountStatus.ACTIVE));
        when(memberRepository.save(any(AccountParticipant.class))).thenReturn(adminMembership(10L));

        var response = useCase.createAccount(new CreateAccountCommand("Home", "Family"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.currentUserRole()).isEqualTo("ACCOUNT_ADMIN");
    }

    @Test
    void listMyAccountsReturnsOnlyRepositoryMemberships() {
        when(memberRepository.findMembershipsForParticipant(10L, PageQuery.of(0, 20)))
                .thenReturn(new PageResponse<>(List.of(adminMembership(10L)), 0, 20, 1, 1));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));

        var response = useCase.listMyAccounts(new ListAccountsQuery(PageQuery.of(0, 20)));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Home");
    }

    @Test
    void getAccountWithoutMembershipReturnsNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAccount(1L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void updateAccountAsAdminWorks() {
        givenAdminAccess();
        when(accountRepository.save(any(Account.class))).thenReturn(Account.restore(1L, "Updated", null, AccountStatus.ACTIVE, Instant.now(), Instant.now()));

        var response = useCase.updateAccount(new UpdateAccountCommand(1L, "Updated", null));

        assertThat(response.name()).isEqualTo("Updated");
    }

    @Test
    void updateAccountAsMemberFails() {
        givenMemberAccess(10L);

        assertThatThrownBy(() -> useCase.updateAccount(new UpdateAccountCommand(1L, "Updated", null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void archivedAccountBlocksUpdate() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.updateAccount(new UpdateAccountCommand(1L, "Updated", null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void archiveAccountAsAdminWorks() {
        givenAdminAccess();
        when(accountRepository.save(any(Account.class))).thenReturn(Account.restore(1L, "Home", null, AccountStatus.ARCHIVED, Instant.now(), Instant.now()));

        var response = useCase.archiveAccount(1L);

        assertThat(response.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void addMemberAsAdminWorks() {
        givenAdminAccess();
        when(participantLookupPort.findActiveByEmail("member@example.com")).thenReturn(Optional.of(new ParticipantInfo(20L, 2L, "member@example.com", "Member", true)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, 20L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(AccountParticipant.class))).thenReturn(memberMembership(20L));

        var response = useCase.addMember(new AddAccountMemberCommand(1L, "member@example.com", AccountParticipantRole.ACCOUNT_MEMBER));

        assertThat(response.participantId()).isEqualTo(20L);
        assertThat(response.role()).isEqualTo("ACCOUNT_MEMBER");
    }

    @Test
    void addMemberAsMemberFails() {
        givenMemberAccess(10L);

        assertThatThrownBy(() -> useCase.addMember(new AddAccountMemberCommand(1L, "member@example.com", AccountParticipantRole.ACCOUNT_MEMBER)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void archivedAccountBlocksAddingMembers() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.addMember(new AddAccountMemberCommand(1L, "member@example.com", AccountParticipantRole.ACCOUNT_MEMBER)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void addDuplicateActiveMemberFails() {
        givenAdminAccess();
        when(participantLookupPort.findActiveByEmail("member@example.com")).thenReturn(Optional.of(new ParticipantInfo(20L, 2L, "member@example.com", "Member", true)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, 20L)).thenReturn(Optional.of(memberMembership(20L)));

        assertThatThrownBy(() -> useCase.addMember(new AddAccountMemberCommand(1L, "member@example.com", AccountParticipantRole.ACCOUNT_MEMBER)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_MEMBER_ALREADY_EXISTS"));
    }

    @Test
    void changeRoleWorksWhenAnotherAdminExists() {
        givenAdminAccess();
        when(memberRepository.findByAccountIdAndParticipantId(1L, 20L)).thenReturn(Optional.of(memberMembership(20L)));
        when(memberRepository.save(any(AccountParticipant.class))).thenReturn(adminMembership(20L));
        when(participantLookupPort.findByParticipantId(20L)).thenReturn(Optional.of(new ParticipantInfo(20L, 2L, "member@example.com", "Member", true)));

        var response = useCase.changeMemberRole(new ChangeAccountMemberRoleCommand(1L, 20L, AccountParticipantRole.ACCOUNT_ADMIN));

        assertThat(response.role()).isEqualTo("ACCOUNT_ADMIN");
        InOrder inOrder = inOrder(memberRepository, accountRepository);
        inOrder.verify(memberRepository).lockByAccountId(1L);
        inOrder.verify(accountRepository).findById(1L);
    }

    @Test
    void archivedAccountBlocksChangingRoles() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.changeMemberRole(new ChangeAccountMemberRoleCommand(1L, 20L, AccountParticipantRole.ACCOUNT_ADMIN)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void cannotDegradeLastAdmin() {
        givenAdminAccess();
        when(memberRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.of(adminMembership(10L)));
        when(memberRepository.countByAccountIdAndRoleAndStatus(1L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.changeMemberRole(new ChangeAccountMemberRoleCommand(1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_LAST_ADMIN_REQUIRED"));
    }

    @Test
    void removeMemberWorks() {
        givenAdminAccess();
        when(memberRepository.findByAccountIdAndParticipantId(1L, 20L)).thenReturn(Optional.of(memberMembership(20L)));
        when(memberRepository.save(any(AccountParticipant.class))).thenReturn(inactiveMember(20L));

        useCase.removeMember(1L, 20L);

        InOrder inOrder = inOrder(memberRepository, accountRepository);
        inOrder.verify(memberRepository).lockByAccountId(1L);
        inOrder.verify(accountRepository).findById(1L);
    }

    @Test
    void archivedAccountBlocksRemovingMembers() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.removeMember(1L, 20L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void cannotRemoveLastAdmin() {
        givenAdminAccess();
        when(memberRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.of(adminMembership(10L)));
        when(memberRepository.countByAccountIdAndRoleAndStatus(1L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.removeMember(1L, 10L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_LAST_ADMIN_REQUIRED"));
    }

    @Test
    void listMembersMapsParticipantDetails() {
        givenAdminAccess();
        when(memberRepository.findByAccountId(1L)).thenReturn(List.of(adminMembership(10L), memberMembership(20L)));
        when(participantLookupPort.findByParticipantIds(Set.of(10L, 20L))).thenReturn(Map.of(
                10L, new ParticipantInfo(10L, 1L, "owner@example.com", "Owner", true),
                20L, new ParticipantInfo(20L, 2L, "member@example.com", "Member", true)
        ));

        var response = useCase.listMembers(1L);

        assertThat(response).hasSize(2);
    }

    @Test
    void listMembersAsMemberHidesInactiveMemberships() {
        givenMemberAccess(10L);
        when(memberRepository.findByAccountId(1L)).thenReturn(List.of(memberMembership(10L), inactiveMember(20L)));
        when(participantLookupPort.findByParticipantIds(Set.of(10L))).thenReturn(Map.of(
                10L, new ParticipantInfo(10L, 1L, "owner@example.com", "Owner", true)
        ));

        var response = useCase.listMembers(1L);

        assertThat(response)
                .hasSize(1)
                .first()
                .extracting("participantId")
                .isEqualTo(10L);
    }

    @Test
    void inactiveMembershipCannotAccessAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.of(inactiveMember(10L)));

        assertThatThrownBy(() -> useCase.getAccount(1L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    private void givenAdminAccess() {
        givenAdminAccess(AccountStatus.ACTIVE);
    }

    private void givenAdminAccess(AccountStatus status) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(status)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.of(adminMembership(10L)));
    }

    private void givenMemberAccess(Long participantId) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(memberRepository.findByAccountIdAndParticipantId(1L, participantId)).thenReturn(Optional.of(memberMembership(participantId)));
    }

    private static Account account(AccountStatus status) {
        return Account.restore(1L, "Home", "Family", status, Instant.now(), Instant.now());
    }

    private static AccountParticipant adminMembership(Long participantId) {
        return AccountParticipant.restore(participantId, 1L, participantId, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), null, null);
    }

    private static AccountParticipant memberMembership(Long participantId) {
        return AccountParticipant.restore(participantId, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null);
    }

    private static AccountParticipant inactiveMember(Long participantId) {
        return AccountParticipant.restore(participantId, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.INACTIVE, Instant.now(), null, null);
    }
}
