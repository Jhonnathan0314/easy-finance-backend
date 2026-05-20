package com.easyfinance.debts.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.port.in.GetDebtPaymentPort;
import com.easyfinance.debts.application.port.in.ListDebtPaymentsPort;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.port.out.DebtPaymentRepositoryPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtPaymentManagementUseCase implements
        RegisterDebtPaymentPort,
        ListDebtPaymentsPort,
        GetDebtPaymentPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final DebtRepositoryPort debtRepository;
    private final DebtPaymentRepositoryPort paymentRepository;
    private final BudgetDebtImpactPort budgetDebtImpactPort;

    public DebtPaymentManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            DebtRepositoryPort debtRepository,
            DebtPaymentRepositoryPort paymentRepository,
            BudgetDebtImpactPort budgetDebtImpactPort
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.debtRepository = debtRepository;
        this.paymentRepository = paymentRepository;
        this.budgetDebtImpactPort = budgetDebtImpactPort;
    }

    @Override
    @Transactional
    public RegisterDebtPaymentResponse registerDebtPayment(RegisterDebtPaymentCommand command) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Debt debt = debtRepository.findByAccountIdAndIdForUpdate(command.accountId(), command.debtId())
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
        DebtPayment payment = DebtPayment.create(
                command.accountId(),
                command.debtId(),
                currentUser.participantId(),
                command.paymentType(),
                command.amount(),
                command.paymentDate(),
                command.notes()
        );
        Debt updatedDebt = debt.applyPayment(payment.amount());
        DebtPayment savedPayment = paymentRepository.save(payment);
        Debt savedDebt = debtRepository.save(updatedDebt);
        if (savedDebt.originExpenseId() != null) {
            budgetDebtImpactPort.applyDebtPaymentToImpacts(new ApplyDebtPaymentImpactCommand(savedDebt.accountId(), savedDebt.id(), savedPayment.amount()));
        }
        return new RegisterDebtPaymentResponse(toPaymentResponse(savedPayment), toDebtResponse(savedDebt));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DebtPaymentResponse> listDebtPayments(ListDebtPaymentsQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        debtRepository.findByAccountIdAndId(query.accountId(), query.debtId())
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
        PageResponse<DebtPayment> page = paymentRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toPaymentResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DebtPaymentResponse getDebtPayment(Long accountId, Long debtId, Long paymentId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        debtRepository.findByAccountIdAndId(accountId, debtId)
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
        return paymentRepository.findByAccountIdAndDebtIdAndId(accountId, debtId, paymentId)
                .map(this::toPaymentResponse)
                .orElseThrow(() -> new NotFoundException("DEBT_PAYMENT_NOT_FOUND", "Debt payment was not found."));
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private Long currentParticipantId() {
        return currentUser().participantId();
    }

    private DebtPaymentResponse toPaymentResponse(DebtPayment payment) {
        return new DebtPaymentResponse(
                payment.id(),
                payment.accountId(),
                payment.debtId(),
                payment.participantId(),
                payment.paymentType().name(),
                payment.amount().amount(),
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
