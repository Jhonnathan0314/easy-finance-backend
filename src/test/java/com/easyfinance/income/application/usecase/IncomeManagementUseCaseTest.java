package com.easyfinance.income.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.income.application.command.CreateIncomeCommand;
import com.easyfinance.income.application.command.DuplicateIncomeCommand;
import com.easyfinance.income.application.command.UpdateIncomeCommand;
import com.easyfinance.income.application.port.out.IncomeRepositoryPort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncomeManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final IncomeRepositoryPort incomeRepository = mock(IncomeRepositoryPort.class);
    private final AccountAuthorizationService accountAuthorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final AssignedParticipantValidator assignedParticipantValidator = new AssignedParticipantValidator(accountParticipantRepository);
    private final IncomeManagementUseCase useCase = new IncomeManagementUseCase(currentUserProvider, accountAuthorizationService, catalogValidationPort, assignedParticipantValidator, incomeRepository);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void memberCreatesIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createIncome(createCommand());

        assertThat(response.participantId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void adminCreatesIncomeAssignedToAnotherActiveParticipant() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenAssignedParticipant(20L, AccountParticipantStatus.ACTIVE);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createIncome(createCommand(20L));

        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void memberCannotCreateIncomeAssignedToAnotherParticipant() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);

        assertThatThrownBy(() -> useCase.createIncome(createCommand(20L)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ALLOWED"));

        verify(incomeRepository, never()).save(any());
    }

    @Test
    void noMemberCannotCreateIncome() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createIncome(createCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void archivedAccountBlocksWrites() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.createIncome(createCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void missingCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createIncome(createCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_CATEGORY_NOT_FOUND"));
    }

    @Test
    void expenseCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L))
                .thenReturn(Optional.of(new CategoryValidationView(2L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.createIncome(createCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_CATEGORY_INVALID_TYPE"));
    }

    @Test
    void inactiveCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.INACTIVE);

        assertThatThrownBy(() -> useCase.createIncome(createCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_CATEGORY_INACTIVE"));
    }

    @Test
    void listOnlyDelegatesAfterMembershipValidation() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findAll(any())).thenReturn(new com.easyfinance.income.application.response.PageResponse<>(List.of(income(5L, 10L, IncomeStatus.ACTIVE)), 0, 20, 1, 1));

        var response = useCase.listIncomes(new ListIncomesQuery(1L, null, null, null, null, null, null, null, null, PageQuery.of(0, 20), null));

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getCrossAccountIncomeFailsAsNotFound() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getIncome(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_NOT_FOUND"));
    }

    @Test
    void ownerUpdatesIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 10L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateIncome(updateCommand());

        assertThat(response.description()).isEqualTo("Updated salary");
    }

    @Test
    void adminUpdatesAnotherParticipantIncome() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateIncome(updateCommand());

        assertThat(response.description()).isEqualTo("Updated salary");
        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void adminReassignsIncomeToAnotherActiveParticipant() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenAssignedParticipant(30L, AccountParticipantStatus.ACTIVE);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateIncome(updateCommand(30L));

        assertThat(response.participantId()).isEqualTo(30L);
    }

    @Test
    void memberCannotReassignIncomeToAnotherParticipant() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 10L, IncomeStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateIncome(updateCommand(20L)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ALLOWED"));

        verify(incomeRepository, never()).save(any());
    }

    @Test
    void anotherMemberCannotUpdateIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateIncome(updateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_UPDATE_NOT_ALLOWED"));
    }

    @Test
    void cancelledIncomeCannotBeUpdated() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.CANCELLED)));

        assertThatThrownBy(() -> useCase.updateIncome(updateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_ALREADY_CANCELLED"));
    }

    @Test
    void ownerCancelsIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 10L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelIncome(1L, 5L);

        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    void adminCancelsAnotherParticipantIncome() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelIncome(1L, 5L);

        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    void ownerDuplicatesIncomeWithOverrides() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        Income source = income(5L, 10L, IncomeStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        var response = useCase.duplicateIncome(new DuplicateIncomeCommand(
                1L,
                5L,
                LocalDate.of(2026, 6, 30),
                Money.cop(new BigDecimal("5200000")),
                "June salary"
        ));

        ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
        verify(incomeRepository).save(captor.capture());
        Income duplicate = captor.getValue();
        assertThat(response.id()).isEqualTo(9L);
        assertThat(duplicate.id()).isNull();
        assertThat(duplicate.categoryId()).isEqualTo(source.categoryId());
        assertThat(duplicate.participantId()).isEqualTo(source.participantId());
        assertThat(duplicate.amount().amount()).isEqualByComparingTo("5200000");
        assertThat(duplicate.description()).isEqualTo("June salary");
        assertThat(duplicate.incomeDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(duplicate.status()).isEqualTo(IncomeStatus.ACTIVE);
        assertThat(source.incomeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void duplicateIncomeUsesSourceAmountAndDescriptionWhenOverridesAreMissing() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        Income source = income(5L, 10L, IncomeStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        useCase.duplicateIncome(new DuplicateIncomeCommand(1L, 5L, LocalDate.of(2026, 6, 30), null, "   "));

        ArgumentCaptor<Income> captor = ArgumentCaptor.forClass(Income.class);
        verify(incomeRepository).save(captor.capture());
        Income duplicate = captor.getValue();
        assertThat(duplicate.amount().amount()).isEqualByComparingTo(source.amount().amount());
        assertThat(duplicate.description()).isEqualTo(source.description());
    }

    @Test
    void adminDuplicatesAnotherParticipantIncome() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenIncomeCategory(CatalogStatus.ACTIVE);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        var response = useCase.duplicateIncome(duplicateCommand());

        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void anotherMemberCannotDuplicateIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.duplicateIncome(duplicateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_DUPLICATE_NOT_ALLOWED"));
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void archivedAccountBlocksDuplicateIncome() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.duplicateIncome(duplicateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void duplicateIncomeFailsWhenSourceDoesNotExist() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.duplicateIncome(duplicateCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_NOT_FOUND"));
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void cancelledIncomeCannotBeDuplicated() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.CANCELLED)));

        assertThatThrownBy(() -> useCase.duplicateIncome(duplicateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_DUPLICATE_NOT_ALLOWED"));
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void anotherMemberCannotCancelIncome() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(incomeRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(income(5L, 20L, IncomeStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.cancelIncome(1L, 5L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_CANCEL_NOT_ALLOWED"));
    }

    private void givenMemberAccess(AccountStatus accountStatus, Long participantId) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, participantId))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenAdminAccess(AccountStatus accountStatus, Long participantId) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, participantId))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, participantId, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenAssignedParticipant(Long participantId, AccountParticipantStatus status) {
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, participantId))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, status, Instant.now(), null, null)));
    }

    private void givenIncomeCategory(CatalogStatus status) {
        when(catalogValidationPort.findCategoryForValidation(1L, 2L))
                .thenReturn(Optional.of(new CategoryValidationView(2L, 1L, CategoryType.INCOME, status)));
    }

    private static CreateIncomeCommand createCommand() {
        return createCommand(null);
    }

    private static CreateIncomeCommand createCommand(Long participantId) {
        return new CreateIncomeCommand(1L, participantId, 2L, "Salary", Money.cop(new BigDecimal("2500000")), LocalDate.of(2026, 5, 10));
    }

    private static UpdateIncomeCommand updateCommand() {
        return updateCommand(null);
    }

    private static UpdateIncomeCommand updateCommand(Long participantId) {
        return new UpdateIncomeCommand(1L, 5L, participantId, 2L, "Updated salary", Money.cop(new BigDecimal("2600000")), LocalDate.of(2026, 5, 11));
    }

    private static DuplicateIncomeCommand duplicateCommand() {
        return new DuplicateIncomeCommand(1L, 5L, LocalDate.of(2026, 6, 30), null, null);
    }

    private static Income persisted(Income income) {
        return Income.restore(5L, income.accountId(), income.categoryId(), income.participantId(), income.description(), income.amount(), income.incomeDate(), income.status(), Instant.now(), Instant.now());
    }

    private static Income persistedWithId(Income income, Long id) {
        return Income.restore(id, income.accountId(), income.categoryId(), income.participantId(), income.description(), income.amount(), income.incomeDate(), income.status(), Instant.now(), Instant.now());
    }

    private static Income income(Long id, Long participantId, IncomeStatus status) {
        return Income.restore(id, 1L, 2L, participantId, "Salary", Money.cop(new BigDecimal("2500000")), LocalDate.of(2026, 5, 10), status, Instant.now(), Instant.now());
    }
}
