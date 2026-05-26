package com.easyfinance.debts.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.port.out.ExpenseOriginValidationPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebtManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final ExpenseOriginValidationPort expenseOriginValidationPort = mock(ExpenseOriginValidationPort.class);
    private final BudgetDebtImpactPort budgetDebtImpactPort = mock(BudgetDebtImpactPort.class);
    private final DebtRepositoryPort debtRepository = mock(DebtRepositoryPort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final DebtManagementUseCase useCase = new DebtManagementUseCase(currentUserProvider, authorizationService, expenseOriginValidationPort, budgetDebtImpactPort, debtRepository);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void memberCreatesManualDebt() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createManualDebt(manualCommand());

        assertThat(response.sourceType()).isEqualTo("MANUAL");
        assertThat(response.remainingAmount()).isEqualByComparingTo(response.totalAmount());
    }

    @Test
    void archivedAccountBlocksManualDebtCreation() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.createManualDebt(manualCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void createDerivedDebtWorks() {
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createInstallmentExpenseDebt(new CreateInstallmentExpenseDebtCommand(1L, 10L, 99L, 7L, "Laptop", null, Money.cop(new BigDecimal("1200000")), 6, Money.cop(new BigDecimal("200000")), LocalDate.of(2026, 6, 1), null));

        assertThat(response.sourceType()).isEqualTo("INSTALLMENT_EXPENSE");
        assertThat(response.totalAmount()).isEqualByComparingTo("1200000.00");
        assertThat(response.scheduledTotalAmount()).isEqualByComparingTo("1200000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("1200000.00");
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        verify(expenseOriginValidationPort).validateInstallmentOrigin(1L, 99L);
    }

    @Test
    void createDerivedDebtSeparatesPrincipalAndScheduledTotal() {
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createInstallmentExpenseDebt(new CreateInstallmentExpenseDebtCommand(1L, 10L, 99L, 7L, "Advance", null, Money.cop(new BigDecimal("1000000")), 12, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 6, 1), null));

        assertThat(response.totalAmount()).isEqualByComparingTo("1000000.00");
        assertThat(response.scheduledTotalAmount()).isEqualByComparingTo("1200000.00");
        assertThat(response.remainingAmount()).isEqualByComparingTo("1000000.00");
        assertThat(response.installmentAmount()).isEqualByComparingTo("100000.00");
        verify(expenseOriginValidationPort).validateInstallmentOrigin(1L, 99L);
    }

    @Test
    void createDerivedDebtWithSimpleOriginFails() {
        doThrow(new BusinessRuleViolationException("DEBT_ORIGIN_EXPENSE_INVALID_TYPE", "Debt origin expense must be an INSTALLMENT expense."))
                .when(expenseOriginValidationPort).validateInstallmentOrigin(1L, 99L);

        assertThatThrownBy(() -> useCase.createInstallmentExpenseDebt(new CreateInstallmentExpenseDebtCommand(1L, 10L, 99L, 7L, "Laptop", null, Money.cop(new BigDecimal("1200000")), 6, Money.cop(new BigDecimal("200000")), LocalDate.of(2026, 6, 1), null)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_ORIGIN_EXPENSE_INVALID_TYPE"));
    }

    @Test
    void createDerivedDebtWithOriginFromAnotherAccountFails() {
        doThrow(new NotFoundException("DEBT_ORIGIN_EXPENSE_NOT_FOUND", "Debt origin expense was not found."))
                .when(expenseOriginValidationPort).validateInstallmentOrigin(1L, 99L);

        assertThatThrownBy(() -> useCase.createInstallmentExpenseDebt(new CreateInstallmentExpenseDebtCommand(1L, 10L, 99L, 7L, "Laptop", null, Money.cop(new BigDecimal("1200000")), 6, Money.cop(new BigDecimal("200000")), LocalDate.of(2026, 6, 1), null)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_ORIGIN_EXPENSE_NOT_FOUND"));
    }

    @Test
    void getDebtOutsideAccountReturnsNotFound() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getDebt(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_NOT_FOUND"));
    }

    @Test
    void cancelByOwnerWorks() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(manualDebt(5L, 10L, DebtState.ACTIVE)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelDebt(1L, 5L);
    }

    @Test
    void cancelByAdminWorksForAnotherParticipantDebt() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(manualDebt(5L, 20L, DebtState.ACTIVE)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelDebt(1L, 5L);
    }

    @Test
    void cancelByAnotherMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(manualDebt(5L, 20L, DebtState.ACTIVE)));

        assertThatThrownBy(() -> useCase.cancelDebt(1L, 5L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_CANCEL_NOT_ALLOWED"));
    }

    @Test
    void cancelDerivedDebtIsBlockedInThisPhase() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(derivedDebt(5L, 20L)));

        assertThatThrownBy(() -> useCase.cancelDebt(1L, 5L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_CANCEL_NOT_ALLOWED"));
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

    private static CreateManualDebtCommand manualCommand() {
        return new CreateManualDebtCommand(1L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.of(2026, 5, 11), null, null);
    }

    private static Debt persisted(Debt debt) {
        return Debt.restore(5L, debt.accountId(), debt.participantId(), debt.originExpenseId(), debt.sourceType(), debt.name(), debt.description(), debt.totalAmount(), debt.scheduledTotalAmount(), debt.remainingBalance(), debt.installmentCount(), debt.installmentAmount(), debt.startDate(), debt.endDate(), debt.state(), debt.notes(), Instant.now(), Instant.now());
    }

    private static Debt manualDebt(Long id, Long participantId, DebtState state) {
        return Debt.restore(id, 1L, participantId, null, DebtSourceType.MANUAL, "Loan", null, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, state, null, Instant.now(), Instant.now());
    }

    private static Debt derivedDebt(Long id, Long participantId) {
        return Debt.restore(id, 1L, participantId, 99L, DebtSourceType.INSTALLMENT_EXPENSE, "Laptop", null, Money.cop(new BigDecimal("1200000")), Money.cop(new BigDecimal("1200000")), Money.cop(new BigDecimal("1200000")), 6, Money.cop(new BigDecimal("200000")), LocalDate.now(), LocalDate.now().plusMonths(6), DebtState.ACTIVE, null, Instant.now(), Instant.now());
    }
}
