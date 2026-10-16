package com.easyfinance.debts.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.debts.application.command.UpdateDebtPaymentCommand;
import com.easyfinance.debts.application.port.in.CancelDebtPaymentPort;
import com.easyfinance.debts.application.port.in.UpdateDebtPaymentPort;
import com.easyfinance.debts.application.port.out.DebtPaymentRepositoryPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.expenses.application.port.in.DebtPaymentExpenseQueryPort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits or cancels a debt payment that has no associated expense. Both operations first revert the
 * original payment (debt balance and budget impacts) and, for edits, apply the new values.
 */
@Service
public class DebtPaymentAdjustmentUseCase implements UpdateDebtPaymentPort, CancelDebtPaymentPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final DebtRepositoryPort debtRepository;
    private final DebtPaymentRepositoryPort paymentRepository;
    private final BudgetDebtImpactPort budgetDebtImpactPort;
    private final DebtPaymentExpenseQueryPort debtPaymentExpenseQueryPort;

    public DebtPaymentAdjustmentUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            DebtRepositoryPort debtRepository,
            DebtPaymentRepositoryPort paymentRepository,
            BudgetDebtImpactPort budgetDebtImpactPort,
            DebtPaymentExpenseQueryPort debtPaymentExpenseQueryPort
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.debtRepository = debtRepository;
        this.paymentRepository = paymentRepository;
        this.budgetDebtImpactPort = budgetDebtImpactPort;
        this.debtPaymentExpenseQueryPort = debtPaymentExpenseQueryPort;
    }

    @Override
    @Transactional
    public RegisterDebtPaymentResponse updateDebtPayment(UpdateDebtPaymentCommand command) {
        Debt debt = lockDebtForAdjustment(command.accountId(), command.debtId());
        DebtPayment payment = requireAdjustablePayment(command.accountId(), command.debtId(), command.paymentId());
        requireRevertible(debt);

        Debt reverted = revert(debt, payment);
        DebtPayment updatedPayment = payment.update(
                command.paymentType(),
                command.capitalAmount(),
                command.interestAmount(),
                command.paymentDate(),
                command.notes()
        );
        Debt updatedDebt = reverted.applyPayment(updatedPayment.capitalAmount());
        DebtPayment savedPayment = paymentRepository.save(updatedPayment);
        Debt savedDebt = debtRepository.save(updatedDebt);
        if (savedDebt.originExpenseId() != null) {
            budgetDebtImpactPort.applyDebtPaymentToImpacts(new ApplyDebtPaymentImpactCommand(
                    savedDebt.accountId(),
                    savedDebt.id(),
                    savedPayment.amount(),
                    savedDebt.state() == DebtState.PAID,
                    savedPayment.paymentDate()
            ));
        }
        return new RegisterDebtPaymentResponse(toPaymentResponse(savedPayment), toDebtResponse(savedDebt));
    }

    @Override
    @Transactional
    public void cancelDebtPayment(Long accountId, Long debtId, Long paymentId) {
        Debt debt = lockDebtForAdjustment(accountId, debtId);
        DebtPayment payment = requireAdjustablePayment(accountId, debtId, paymentId);
        requireRevertible(debt);

        Debt reverted = revert(debt, payment);
        paymentRepository.save(payment.cancel());
        debtRepository.save(reverted);
    }

    private Debt revert(Debt debt, DebtPayment payment) {
        Debt reverted = debt.revertPayment(payment.capitalAmount());
        if (debt.originExpenseId() != null) {
            budgetDebtImpactPort.revertDebtPaymentFromImpacts(debt.accountId(), debt.id(), payment.amount());
        }
        return reverted;
    }

    private Debt lockDebtForAdjustment(Long accountId, Long debtId) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveMemberForActiveAccount(accountId, currentUser.participantId());
        return debtRepository.findByAccountIdAndIdForUpdate(accountId, debtId)
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
    }

    private DebtPayment requireAdjustablePayment(Long accountId, Long debtId, Long paymentId) {
        DebtPayment payment = paymentRepository.findByAccountIdAndDebtIdAndId(accountId, debtId, paymentId)
                .orElseThrow(() -> new NotFoundException("DEBT_PAYMENT_NOT_FOUND", "Debt payment was not found."));
        if (payment.status() != DebtPaymentStatus.ACTIVE) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_NOT_ACTIVE", "Debt payment is already cancelled.");
        }
        if (debtPaymentExpenseQueryPort.hasActiveExpenseForDebtPayment(accountId, paymentId)) {
            throw new BusinessRuleViolationException(
                    "DEBT_PAYMENT_HAS_EXPENSE",
                    "A debt payment with an associated expense cannot be edited or deleted from the debt."
            );
        }
        return payment;
    }

    private static void requireRevertible(Debt debt) {
        if (debt.state() == DebtState.CANCELLED) {
            throw new BusinessRuleViolationException("DEBT_CANCELLED", "Debt is cancelled.");
        }
        if (debt.state() == DebtState.PAID && debt.originExpenseId() != null) {
            throw new BusinessRuleViolationException(
                    "DEBT_PAYMENT_REVERT_NOT_SUPPORTED",
                    "Payments that settled an installment debt linked to budgets cannot be edited or deleted."
            );
        }
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private DebtPaymentResponse toPaymentResponse(DebtPayment payment) {
        return new DebtPaymentResponse(
                payment.id(),
                payment.accountId(),
                payment.debtId(),
                payment.participantId(),
                payment.paymentType().name(),
                payment.amount().amount(),
                payment.capitalAmount().amount(),
                payment.interestAmount().amount(),
                payment.amount().currency().name(),
                payment.paymentDate(),
                payment.notes(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }

    private DebtResponse toDebtResponse(Debt debt) {
        return new DebtResponse(
                debt.id(),
                debt.accountId(),
                debt.participantId(),
                debt.originExpenseId(),
                debt.sourceType().name(),
                debt.name(),
                debt.description(),
                debt.totalAmount().amount(),
                debt.scheduledTotalAmount().amount(),
                debt.totalAmount().currency().name(),
                debt.remainingBalance().amount(),
                debt.remainingBalance().currency().name(),
                debt.installmentCount(),
                debt.installmentAmount() == null ? null : debt.installmentAmount().amount(),
                debt.installmentAmount() == null ? null : debt.installmentAmount().currency().name(),
                debt.startDate(),
                debt.endDate(),
                debt.state().name(),
                debt.notes(),
                debt.createdAt(),
                debt.updatedAt()
        );
    }
}
