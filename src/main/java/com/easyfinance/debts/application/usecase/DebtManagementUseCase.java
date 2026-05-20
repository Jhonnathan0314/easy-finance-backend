package com.easyfinance.debts.application.usecase;

import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.port.in.CancelDebtPort;
import com.easyfinance.debts.application.port.in.CreateInstallmentExpenseDebtPort;
import com.easyfinance.debts.application.port.in.CreateManualDebtPort;
import com.easyfinance.debts.application.port.in.GetDebtPort;
import com.easyfinance.debts.application.port.in.ListDebtsPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.port.out.ExpenseOriginValidationPort;
import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtManagementUseCase implements
        CreateManualDebtPort,
        CreateInstallmentExpenseDebtPort,
        ListDebtsPort,
        GetDebtPort,
        CancelDebtPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final ExpenseOriginValidationPort expenseOriginValidationPort;
    private final BudgetDebtImpactPort budgetDebtImpactPort;
    private final DebtRepositoryPort debtRepository;

    public DebtManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            ExpenseOriginValidationPort expenseOriginValidationPort,
            BudgetDebtImpactPort budgetDebtImpactPort,
            DebtRepositoryPort debtRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.expenseOriginValidationPort = expenseOriginValidationPort;
        this.budgetDebtImpactPort = budgetDebtImpactPort;
        this.debtRepository = debtRepository;
    }

    @Override
    @Transactional
    public DebtResponse createManualDebt(CreateManualDebtCommand command) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Debt debt = Debt.createManual(
                command.accountId(),
                currentUser.participantId(),
                command.name(),
                command.description(),
                command.totalAmount(),
                command.installmentCount(),
                command.installmentAmount(),
                command.startDate(),
                command.dueDate(),
                command.notes()
        );
        return toResponse(debtRepository.save(debt));
    }

    @Override
    @Transactional
    public DebtResponse createInstallmentExpenseDebt(CreateInstallmentExpenseDebtCommand command) {
        expenseOriginValidationPort.validateInstallmentOrigin(command.accountId(), command.originExpenseId());
        Debt debt = Debt.createFromInstallmentExpense(
                command.accountId(),
                command.participantId(),
                command.originExpenseId(),
                command.name(),
                command.description(),
                command.totalAmount(),
                command.installmentCount(),
                command.installmentAmount(),
                command.firstInstallmentDate(),
                command.notes()
        );
        Debt savedDebt = debtRepository.save(debt);
        budgetDebtImpactPort.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(
                savedDebt.accountId(),
                savedDebt.id(),
                savedDebt.originExpenseId(),
                command.categoryId(),
                savedDebt.name(),
                savedDebt.totalAmount(),
                savedDebt.installmentCount(),
                savedDebt.installmentAmount(),
                savedDebt.startDate()
        ));
        return toResponse(savedDebt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DebtResponse> listDebts(ListDebtsQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<Debt> page = debtRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DebtResponse getDebt(Long accountId, Long debtId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        return toResponse(findDebt(accountId, debtId));
    }

    @Override
    @Transactional
    public void cancelDebt(Long accountId, Long debtId) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(accountId, currentUser.participantId());
        Debt debt = findDebt(accountId, debtId);
        debt.ensureActive();
        if (debt.sourceType() == DebtSourceType.INSTALLMENT_EXPENSE) {
            throw new ForbiddenOperationException("DEBT_CANCEL_NOT_ALLOWED", "Installment expense debt cancellation is not available in this phase.");
        }
        ensureCanCancel(debt, access, currentUser.participantId());
        debtRepository.save(debt.cancel());
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private Long currentParticipantId() {
        return currentUser().participantId();
    }

    private Debt findDebt(Long accountId, Long debtId) {
        return debtRepository.findByAccountIdAndId(accountId, debtId)
                .orElseThrow(() -> new NotFoundException("DEBT_NOT_FOUND", "Debt was not found."));
    }

    private void ensureCanCancel(Debt debt, AccountAccess access, Long participantId) {
        if (debt.participantId().equals(participantId) || access.membership().role() == AccountParticipantRole.ACCOUNT_ADMIN) {
            return;
        }
        throw new ForbiddenOperationException("DEBT_CANCEL_NOT_ALLOWED", "Debt cancellation is not allowed.");
    }

    private DebtResponse toResponse(Debt debt) {
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
