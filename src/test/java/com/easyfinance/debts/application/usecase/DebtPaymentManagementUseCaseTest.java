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
import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.port.out.DebtPaymentRepositoryPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.expenses.application.port.in.CreateDebtPaymentExpensePort;
import com.easyfinance.expenses.application.response.ExpenseResponse;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebtPaymentManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final DebtRepositoryPort debtRepository = mock(DebtRepositoryPort.class);
    private final DebtPaymentRepositoryPort paymentRepository = mock(DebtPaymentRepositoryPort.class);
    private final BudgetDebtImpactPort budgetDebtImpactPort = mock(BudgetDebtImpactPort.class);
    private final CreateDebtPaymentExpensePort createDebtPaymentExpensePort = mock(CreateDebtPaymentExpensePort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final DebtPaymentManagementUseCase useCase = new DebtPaymentManagementUseCase(currentUserProvider, authorizationService, debtRepository, paymentRepository, budgetDebtImpactPort, createDebtPaymentExpensePort);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void memberRegistersPartialPaymentAndDebtRemainsActive() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.registerDebtPayment(command(new BigDecimal("40000")));

        assertThat(response.payment().amount()).isEqualByComparingTo("40000.00");
        assertThat(response.debt().remainingAmount()).isEqualByComparingTo("60000.00");
        assertThat(response.debt().state()).isEqualTo("ACTIVE");
        assertThat(response.createdExpenseId()).isNull();
        verify(debtRepository).findByAccountIdAndIdForUpdate(1L, 5L);
        verify(createDebtPaymentExpensePort, never()).createDebtPaymentExpense(any());
    }

    @Test
    void paymentWithAssociatedExpenseCreatesDebtPaymentExpense() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(createDebtPaymentExpensePort.createDebtPaymentExpense(any())).thenReturn(expenseResponse(700L));

        var response = useCase.registerDebtPayment(commandWithExpense(new BigDecimal("40000")));

        ArgumentCaptor<com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand> captor =
                ArgumentCaptor.forClass(com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand.class);
        verify(createDebtPaymentExpensePort).createDebtPaymentExpense(captor.capture());
        assertThat(response.createdExpenseId()).isEqualTo(700L);
        assertThat(captor.getValue().accountId()).isEqualTo(1L);
        assertThat(captor.getValue().categoryId()).isEqualTo(2L);
        assertThat(captor.getValue().paymentMethodId()).isEqualTo(3L);
        assertThat(captor.getValue().participantId()).isEqualTo(10L);
        assertThat(captor.getValue().debtPaymentId()).isEqualTo(50L);
        assertThat(captor.getValue().description()).isEqualTo("Debt payment expense");
        assertThat(captor.getValue().amount().amount()).isEqualByComparingTo("40000");
        assertThat(captor.getValue().expenseDate()).isEqualTo(LocalDate.of(2026, 5, 11));
    }

    @Test
    void paymentWithAssociatedExpenseRequiresCategory() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> useCase.registerDebtPayment(new RegisterDebtPaymentCommand(
                1L, 5L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("40000")),
                LocalDate.of(2026, 5, 11), null, true, null, 3L, "Debt payment expense"
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXPENSE_CATEGORY_REQUIRED"));
        verify(createDebtPaymentExpensePort, never()).createDebtPaymentExpense(any());
    }

    @Test
    void paymentWithAssociatedExpenseRequiresPaymentMethod() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> useCase.registerDebtPayment(new RegisterDebtPaymentCommand(
                1L, 5L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("40000")),
                LocalDate.of(2026, 5, 11), null, true, 2L, null, "Debt payment expense"
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXPENSE_PAYMENT_METHOD_REQUIRED"));
        verify(createDebtPaymentExpensePort, never()).createDebtPaymentExpense(any());
    }

    @Test
    void paymentWithAssociatedExpenseRequiresDescription() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> useCase.registerDebtPayment(new RegisterDebtPaymentCommand(
                1L, 5L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("40000")),
                LocalDate.of(2026, 5, 11), null, true, 2L, 3L, " "
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXPENSE_DESCRIPTION_REQUIRED"));
        verify(createDebtPaymentExpensePort, never()).createDebtPaymentExpense(any());
    }

    @Test
    void totalPaymentMarksDebtPaid() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.registerDebtPayment(command(new BigDecimal("100000")));

        assertThat(response.debt().remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(response.debt().state()).isEqualTo("PAID");
    }

    @Test
    void archivedAccountBlocksPayment() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("10000"))))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void debtNotFoundFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("10000"))))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_NOT_FOUND"));
    }

    @Test
    void overpaymentFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("100001"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE"));
        verify(paymentRepository, never()).save(any());
        verify(debtRepository, never()).save(any());
    }

    @Test
    void capitalPaymentCanExceedInstallmentAmountWhenWithinFinancedRemainingBalance() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        Debt derivedDebt = derivedDebt(
                Money.cop(new BigDecimal("1200000")),
                Money.cop(new BigDecimal("1200000")),
                Money.cop(new BigDecimal("100000")),
                DebtState.ACTIVE
        );
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(derivedDebt));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterDebtPaymentCommand capitalCommand = new RegisterDebtPaymentCommand(
                1L,
                5L,
                DebtPaymentType.CAPITAL_PAYMENT,
                Money.cop(new BigDecimal("300000")),
                LocalDate.of(2026, 5, 11),
                "Capital prepayment"
        );

        var response = useCase.registerDebtPayment(capitalCommand);

        assertThat(response.payment().paymentType()).isEqualTo("CAPITAL_PAYMENT");
        assertThat(response.debt().remainingAmount()).isEqualByComparingTo("900000.00");
        assertThat(response.debt().state()).isEqualTo("ACTIVE");
        verify(budgetDebtImpactPort).applyDebtPaymentToImpacts(any());
    }

    @Test
    void capitalPaymentOverFinancedRemainingBalanceFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        Debt derivedDebt = derivedDebt(
                Money.cop(new BigDecimal("1200000")),
                Money.cop(new BigDecimal("900000")),
                Money.cop(new BigDecimal("100000")),
                DebtState.ACTIVE
        );
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(derivedDebt));

        RegisterDebtPaymentCommand capitalCommand = new RegisterDebtPaymentCommand(
                1L,
                5L,
                DebtPaymentType.CAPITAL_PAYMENT,
                Money.cop(new BigDecimal("900001")),
                LocalDate.of(2026, 5, 11),
                "Capital prepayment"
        );

        assertThatThrownBy(() -> useCase.registerDebtPayment(capitalCommand))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE"));
        verify(paymentRepository, never()).save(any());
        verify(debtRepository, never()).save(any());
    }

    @Test
    void paidDebtRejectsPayment() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.zeroCop(), DebtState.PAID)));

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("1"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_ALREADY_PAID"));
    }

    @Test
    void cancelledDebtRejectsPayment() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.CANCELLED)));

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("1"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_CANCELLED"));
    }

    @Test
    void ifPaymentSaveFailsDebtIsNotUpdated() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        doThrow(new IllegalStateException("payment save failed")).when(paymentRepository).save(any());

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("10000"))))
                .isInstanceOf(IllegalStateException.class);
        verify(debtRepository, never()).save(any());
    }

    @Test
    void ifDebtSaveFailsExceptionPropagatesForTransactionRollback() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> persistedPayment(invocation.getArgument(0)));
        doThrow(new IllegalStateException("debt save failed")).when(debtRepository).save(any());

        assertThatThrownBy(() -> useCase.registerDebtPayment(command(new BigDecimal("10000"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listPaymentsRequiresDebtInAccount() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.findAll(any())).thenReturn(new PageResponse<>(List.of(payment()), 0, 20, 1, 1));

        var response = useCase.listDebtPayments(new ListDebtPaymentsQuery(1L, 5L, null, null, null, null, PageQuery.of(null, null), null));

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getPaymentCrossAccountFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(debtRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(debt(Money.cop(new BigDecimal("100000")), DebtState.ACTIVE)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getDebtPayment(1L, 5L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_NOT_FOUND"));
    }

    private void givenMemberAccess(AccountStatus accountStatus, Long participantId) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, participantId))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private static RegisterDebtPaymentCommand command(BigDecimal amount) {
        return new RegisterDebtPaymentCommand(1L, 5L, DebtPaymentType.INSTALLMENT, Money.cop(amount), LocalDate.of(2026, 5, 11), null);
    }

    private static RegisterDebtPaymentCommand commandWithExpense(BigDecimal amount) {
        return new RegisterDebtPaymentCommand(
                1L,
                5L,
                DebtPaymentType.INSTALLMENT,
                Money.cop(amount),
                LocalDate.of(2026, 5, 11),
                null,
                true,
                2L,
                3L,
                "Debt payment expense"
        );
    }

    private static ExpenseResponse expenseResponse(Long id) {
        return new ExpenseResponse(
                id,
                1L,
                2L,
                3L,
                10L,
                "Debt payment expense",
                new BigDecimal("40000.00"),
                "COP",
                LocalDate.of(2026, 5, 11),
                "PAID",
                "ACTIVE",
                "SIMPLE",
                "DEBT_PAYMENT",
                50L,
                Instant.now(),
                Instant.now()
        );
    }

    private static Debt debt(Money remainingBalance, DebtState state) {
        return Debt.restore(5L, 1L, 20L, null, DebtSourceType.MANUAL, "Loan", null, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), remainingBalance, null, null, LocalDate.now(), null, state, null, Instant.now(), Instant.now());
    }

    private static Debt derivedDebt(Money totalAmount, Money remainingBalance, Money installmentAmount, DebtState state) {
        return Debt.restore(
                5L,
                1L,
                20L,
                99L,
                DebtSourceType.INSTALLMENT_EXPENSE,
                "Financed purchase",
                null,
                totalAmount,
                totalAmount,
                remainingBalance,
                12,
                installmentAmount,
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2027, 5, 11),
                state,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private static DebtPayment persistedPayment(DebtPayment payment) {
        return DebtPayment.restore(50L, payment.accountId(), payment.debtId(), payment.participantId(), payment.paymentType(), payment.amount(), payment.paymentDate(), payment.notes(), payment.status(), Instant.now(), Instant.now());
    }

    private static DebtPayment payment() {
        return DebtPayment.restore(50L, 1L, 5L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("50000")), LocalDate.now(), null, DebtPaymentStatus.ACTIVE, Instant.now(), Instant.now());
    }
}
