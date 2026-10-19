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
import com.easyfinance.debts.application.command.UpdateDebtPaymentCommand;
import com.easyfinance.debts.application.port.out.DebtPaymentRepositoryPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.expenses.application.port.in.DebtPaymentExpenseSyncPort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebtPaymentAdjustmentUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final DebtRepositoryPort debtRepository = mock(DebtRepositoryPort.class);
    private final DebtPaymentRepositoryPort paymentRepository = mock(DebtPaymentRepositoryPort.class);
    private final BudgetDebtImpactPort budgetDebtImpactPort = mock(BudgetDebtImpactPort.class);
    private final DebtPaymentExpenseSyncPort expenseSyncPort = mock(DebtPaymentExpenseSyncPort.class);
    private final DebtPaymentAdjustmentUseCase useCase = new DebtPaymentAdjustmentUseCase(
            currentUserProvider,
            new AccountAuthorizationService(accountRepository, accountParticipantRepository),
            debtRepository,
            paymentRepository,
            budgetDebtImpactPort,
            expenseSyncPort
    );

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
        when(paymentRepository.save(any(DebtPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cancellingAPaymentRestoresTheDebtBalanceAndReactivatesAPaidDebt() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("0", DebtState.PAID, null)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("100000")));

        useCase.cancelDebtPayment(1L, 5L, 50L);

        ArgumentCaptor<Debt> debtCaptor = ArgumentCaptor.forClass(Debt.class);
        verify(debtRepository).save(debtCaptor.capture());
        assertThat(debtCaptor.getValue().remainingBalance().amount()).isEqualByComparingTo("100000");
        assertThat(debtCaptor.getValue().state()).isEqualTo(DebtState.ACTIVE);
        ArgumentCaptor<DebtPayment> paymentCaptor = ArgumentCaptor.forClass(DebtPayment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().status()).isEqualTo(DebtPaymentStatus.CANCELLED);
        verify(budgetDebtImpactPort, never()).revertDebtPaymentFromImpacts(any(), any(), any());
    }

    @Test
    void updatingAPaymentRevertsTheOldAmountAndAppliesTheNewOne() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("60000", DebtState.ACTIVE, null)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("40000")));

        var response = useCase.updateDebtPayment(new UpdateDebtPaymentCommand(
                1L, 5L, 50L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("25000")), Money.zeroCop(), LocalDate.of(2026, 6, 1), "Corrected"));

        assertThat(response.payment().capitalAmount()).isEqualByComparingTo("25000");
        assertThat(response.payment().notes()).isEqualTo("Corrected");
        assertThat(response.debt().remainingAmount()).isEqualByComparingTo("75000");
        assertThat(response.debt().state()).isEqualTo("ACTIVE");
    }

    @Test
    void cancellingAPaymentAlsoCancelsItsAssociatedExpense() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("60000", DebtState.ACTIVE, null)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("40000")));

        useCase.cancelDebtPayment(1L, 5L, 50L);

        verify(expenseSyncPort).cancelExpenseForDebtPayment(1L, 50L);
    }

    @Test
    void updatingAPaymentSyncsItsAssociatedExpenseAmountAndDate() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("60000", DebtState.ACTIVE, null)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("40000")));

        useCase.updateDebtPayment(new UpdateDebtPaymentCommand(
                1L, 5L, 50L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("25000")), Money.cop(new BigDecimal("1000")), LocalDate.of(2026, 6, 1), null));

        verify(expenseSyncPort).updateExpenseForDebtPayment(1L, 50L, Money.cop(new BigDecimal("26000")), LocalDate.of(2026, 6, 1));
    }

    @Test
    void cancellingAPaymentOnABudgetLinkedDebtRevertsTheBudgetImpacts() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("60000", DebtState.ACTIVE, 99L)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("40000")));

        useCase.cancelDebtPayment(1L, 5L, 50L);

        verify(budgetDebtImpactPort).revertDebtPaymentFromImpacts(any(), any(), any());
    }

    @Test
    void settledBudgetLinkedDebtsCannotRevertPayments() {
        when(debtRepository.findByAccountIdAndIdForUpdate(1L, 5L)).thenReturn(Optional.of(debt("0", DebtState.PAID, 99L)));
        when(paymentRepository.findByAccountIdAndDebtIdAndId(1L, 5L, 50L)).thenReturn(Optional.of(payment("100000")));

        assertThatThrownBy(() -> useCase.cancelDebtPayment(1L, 5L, 50L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_REVERT_NOT_SUPPORTED"));
    }

    private static Debt debt(String remaining, DebtState state, Long originExpenseId) {
        boolean derived = originExpenseId != null;
        return Debt.restore(5L, 1L, 20L, originExpenseId, derived ? DebtSourceType.INSTALLMENT_EXPENSE : DebtSourceType.MANUAL, "Loan", null,
                Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal(remaining)),
                derived ? 10 : null, derived ? Money.cop(new BigDecimal("10000")) : null, LocalDate.of(2026, 5, 1),
                derived ? LocalDate.of(2027, 3, 1) : null, state, null, Instant.now(), Instant.now());
    }

    private static DebtPayment payment(String capital) {
        return DebtPayment.restore(50L, 1L, 5L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal(capital)), Money.zeroCop(),
                LocalDate.of(2026, 5, 11), null, DebtPaymentStatus.ACTIVE, Instant.now(), Instant.now());
    }
}
