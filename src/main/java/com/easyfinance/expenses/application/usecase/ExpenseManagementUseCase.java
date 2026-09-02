package com.easyfinance.expenses.application.usecase;

import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.port.in.CreateInstallmentExpenseDebtPort;
import com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand;
import com.easyfinance.expenses.application.command.CreateExpenseCommand;
import com.easyfinance.expenses.application.command.CreateImportedExpenseCommand;
import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.command.DuplicateExpenseCommand;
import com.easyfinance.expenses.application.command.UpdateExpenseCommand;
import com.easyfinance.expenses.application.port.in.CancelExpensePort;
import com.easyfinance.expenses.application.port.in.CreateDebtPaymentExpensePort;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.expenses.application.port.in.CreateImportedExpensePort;
import com.easyfinance.expenses.application.port.in.CreateExpensePort;
import com.easyfinance.expenses.application.port.in.DuplicateExpensePort;
import com.easyfinance.expenses.application.port.in.GetExpensePort;
import com.easyfinance.expenses.application.port.in.ListExpensesPort;
import com.easyfinance.expenses.application.port.in.UpdateExpensePort;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpenseSourceType;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.DomainException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ExpenseManagementUseCase implements
        CreateExpensePort,
        CreateDebtPaymentExpensePort,
        CreateImportedExpensePort,
        CreateInstallmentExpensePort,
        ListExpensesPort,
        GetExpensePort,
        UpdateExpensePort,
        CancelExpensePort,
        DuplicateExpensePort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final CreateInstallmentExpenseDebtPort createInstallmentExpenseDebtPort;
    private final ExpenseRepositoryPort expenseRepository;

    public ExpenseManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            AssignedParticipantValidator assignedParticipantValidator,
            CreateInstallmentExpenseDebtPort createInstallmentExpenseDebtPort,
            ExpenseRepositoryPort expenseRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.createInstallmentExpenseDebtPort = createInstallmentExpenseDebtPort;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(CreateExpenseCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Long assignedParticipantId = assignedParticipantValidator.resolveAssignedParticipantId(access, command.participantId());
        validateCategory(command.accountId(), command.categoryId());
        validatePaymentMethod(command.accountId(), command.paymentMethodId());
        Expense expense = Expense.createSimple(
                command.accountId(),
                command.categoryId(),
                command.paymentMethodId(),
                assignedParticipantId,
                command.description(),
                command.amount(),
                command.expenseDate(),
                command.paymentState()
        );
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse createImportedExpense(CreateImportedExpenseCommand command) {
        validateCategory(command.accountId(), command.categoryId());
        validatePaymentMethod(command.accountId(), command.paymentMethodId());
        Expense expense = Expense.createImported(
                command.accountId(),
                command.categoryId(),
                command.paymentMethodId(),
                command.participantId(),
                command.description(),
                command.amount(),
                command.expenseDate(),
                command.paymentState()
        );
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse createDebtPaymentExpense(CreateDebtPaymentExpenseCommand command) {
        validateCategory(command.accountId(), command.categoryId());
        validatePaymentMethod(command.accountId(), command.paymentMethodId());
        Expense expense = Expense.createDebtPayment(
                command.accountId(),
                command.categoryId(),
                command.paymentMethodId(),
                command.participantId(),
                command.debtId(),
                command.debtPaymentId(),
                command.description(),
                command.amount(),
                command.expenseDate()
        );
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse createInstallmentExpense(CreateInstallmentExpenseCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Long assignedParticipantId = assignedParticipantValidator.resolveAssignedParticipantId(access, command.participantId());
        validateCategory(command.accountId(), command.categoryId());
        validatePaymentMethod(command.accountId(), command.paymentMethodId());
        validateInstallmentData(command.installmentCount(), command.installmentAmount(), command.firstInstallmentDate());
        financedTotal(command.installmentAmount(), command.installmentCount(), command.totalAmount());
        Expense expense = Expense.createInstallment(
                command.accountId(),
                command.categoryId(),
                command.paymentMethodId(),
                assignedParticipantId,
                command.description(),
                command.totalAmount(),
                command.expenseDate()
        );
        Expense savedExpense = expenseRepository.save(expense);
        try {
            createInstallmentExpenseDebtPort.createInstallmentExpenseDebt(new CreateInstallmentExpenseDebtCommand(
                    command.accountId(),
                    assignedParticipantId,
                    savedExpense.id(),
                    savedExpense.categoryId(),
                    command.debtName() == null || command.debtName().isBlank() ? command.description() : command.debtName(),
                    command.description(),
                    command.totalAmount(),
                    command.installmentCount(),
                    command.installmentAmount(),
                    command.firstInstallmentDate(),
                    command.notes()
            ));
        } catch (DomainException ex) {
            throw new BusinessRuleViolationException("INSTALLMENT_EXPENSE_DEBT_CREATION_FAILED", "Installment expense debt could not be created.", ex);
        }
        return toResponse(savedExpense);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> listExpenses(ListExpensesQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<Expense> page = expenseRepository.findAll(query);
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
    public ExpenseResponse getExpense(Long accountId, Long expenseId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        return toResponse(findExpense(accountId, expenseId));
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(UpdateExpenseCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Expense expense = findExpense(command.accountId(), command.expenseId());
        expense.ensureActive();
        ensureSimpleExpenseLifecycleOperation(expense, "INSTALLMENT_EXPENSE_UPDATE_NOT_ALLOWED", "Installment expense update is not available in this phase.");
        ensureNotDebtPaymentOrigin(expense, "EXPENSE_DEBT_PAYMENT_UPDATE_NOT_ALLOWED", "Expense originated from a debt payment and must be managed from the associated debt.");
        ensureCanMutate(expense, access, currentUser.participantId(), "EXPENSE_UPDATE_NOT_ALLOWED");
        validateCategory(command.accountId(), command.categoryId());
        validatePaymentMethod(command.accountId(), command.paymentMethodId());
        Long assignedParticipantId = command.participantId() == null
                ? expense.participantId()
                : assignedParticipantValidator.resolveAssignedParticipantId(access, command.participantId());
        Expense updated = expense.update(
                command.categoryId(),
                command.paymentMethodId(),
                assignedParticipantId,
                command.description(),
                command.amount(),
                command.expenseDate(),
                command.paymentState()
        );
        return toResponse(expenseRepository.save(updated));
    }

    @Override
    @Transactional
    public ExpenseResponse duplicateExpense(DuplicateExpenseCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Expense source = findExpense(command.accountId(), command.expenseId());
        ensureCanMutate(source, access, currentUser.participantId(), "EXPENSE_DUPLICATE_NOT_ALLOWED");
        if (source.status() != ExpenseStatus.ACTIVE) {
            throw new BusinessRuleViolationException("EXPENSE_DUPLICATE_NOT_ALLOWED", "Only active expenses can be duplicated.");
        }
        if (source.expenseType() != ExpenseType.SIMPLE) {
            throw new BusinessRuleViolationException("EXPENSE_DUPLICATE_NOT_ALLOWED", "Only simple expenses can be duplicated.");
        }
        validateCategory(command.accountId(), source.categoryId());
        validatePaymentMethod(command.accountId(), source.paymentMethodId());
        Expense duplicate = Expense.createSimple(
                source.accountId(),
                source.categoryId(),
                source.paymentMethodId(),
                source.participantId(),
                command.description() == null || command.description().isBlank() ? source.description() : command.description(),
                command.amount() == null ? source.amount() : command.amount(),
                command.expenseDate(),
                command.paymentState() == null ? source.paymentState() : command.paymentState()
        );
        return toResponse(expenseRepository.save(duplicate));
    }

    @Override
    @Transactional
    public void cancelExpense(Long accountId, Long expenseId) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(accountId, currentUser.participantId());
        Expense expense = findExpense(accountId, expenseId);
        expense.ensureActive();
        ensureSimpleExpenseLifecycleOperation(expense, "INSTALLMENT_EXPENSE_CANCEL_NOT_ALLOWED", "Installment expense cancellation is not available in this phase.");
        ensureNotDebtPaymentOrigin(expense, "EXPENSE_DEBT_PAYMENT_CANCEL_NOT_ALLOWED", "Expense originated from a debt payment and must be managed from the associated debt.");
        ensureCanMutate(expense, access, currentUser.participantId(), "EXPENSE_CANCEL_NOT_ALLOWED");
        expenseRepository.save(expense.cancel());
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private Long currentParticipantId() {
        return currentUser().participantId();
    }

    private Expense findExpense(Long accountId, Long expenseId) {
        return expenseRepository.findByAccountIdAndId(accountId, expenseId)
                .orElseThrow(() -> new NotFoundException("EXPENSE_NOT_FOUND", "Expense was not found."));
    }

    private void ensureCanMutate(Expense expense, AccountAccess access, Long participantId, String errorCode) {
        if (expense.participantId().equals(participantId) || access.membership().role() == AccountParticipantRole.ACCOUNT_ADMIN) {
            return;
        }
        throw new ForbiddenOperationException(errorCode, "Expense operation is not allowed.");
    }

    private void ensureSimpleExpenseLifecycleOperation(Expense expense, String code, String message) {
        if (expense.expenseType() == ExpenseType.INSTALLMENT) {
            throw new BusinessRuleViolationException(code, message);
        }
    }

    private void ensureNotDebtPaymentOrigin(Expense expense, String code, String message) {
        if (expense.sourceType() == ExpenseSourceType.DEBT_PAYMENT) {
            throw new BusinessRuleViolationException(code, message);
        }
    }

    private void validateCategory(Long accountId, Long categoryId) {
        if (categoryId == null) {
            throw new BusinessRuleViolationException("EXPENSE_CATEGORY_REQUIRED", "Expense category is required.");
        }
        CategoryValidationView category = catalogValidationPort.findCategoryForValidation(accountId, categoryId)
                .orElseThrow(() -> new NotFoundException("EXPENSE_CATEGORY_NOT_FOUND", "Expense category was not found."));
        if (category.status() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("EXPENSE_CATEGORY_INACTIVE", "Expense category is inactive.");
        }
        if (category.type() != CategoryType.EXPENSE) {
            throw new BusinessRuleViolationException("EXPENSE_CATEGORY_INVALID_TYPE", "Expense category must be an EXPENSE category.");
        }
    }

    private void validatePaymentMethod(Long accountId, Long paymentMethodId) {
        if (paymentMethodId == null) {
            throw new BusinessRuleViolationException("EXPENSE_PAYMENT_METHOD_REQUIRED", "Expense payment method is required.");
        }
        PaymentMethodValidationView paymentMethod = catalogValidationPort.findPaymentMethodForValidation(accountId, paymentMethodId)
                .orElseThrow(() -> new NotFoundException("EXPENSE_PAYMENT_METHOD_NOT_FOUND", "Expense payment method was not found."));
        if (paymentMethod.status() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("EXPENSE_PAYMENT_METHOD_INACTIVE", "Expense payment method is inactive.");
        }
    }

    private void validateInstallmentData(Integer installmentCount, Money installmentAmount, java.time.LocalDate firstInstallmentDate) {
        if (installmentCount == null || installmentCount <= 0) {
            throw new BusinessRuleViolationException("DEBT_INSTALLMENT_COUNT_INVALID", "Installment count must be greater than zero.");
        }
        if (installmentAmount == null || installmentAmount.amount().signum() <= 0) {
            throw new BusinessRuleViolationException("DEBT_INSTALLMENT_AMOUNT_INVALID", "Installment amount must be greater than zero.");
        }
        if (firstInstallmentDate == null) {
            throw new BusinessRuleViolationException("INSTALLMENT_EXPENSE_INVALID", "First installment date is required.");
        }
    }

    private Money financedTotal(Money installmentAmount, Integer installmentCount, Money originalTotalAmount) {
        BigDecimal financedAmount = installmentAmount.amount().multiply(BigDecimal.valueOf(installmentCount));
        if (financedAmount.compareTo(originalTotalAmount.amount()) < 0) {
            throw new BusinessRuleViolationException("INSTALLMENT_FINANCED_TOTAL_INVALID", "Installment total cannot be lower than original expense amount.");
        }
        return Money.cop(financedAmount);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.id(),
                expense.accountId(),
                expense.categoryId(),
                expense.paymentMethodId(),
                expense.participantId(),
                expense.description(),
                expense.amount().amount(),
                expense.amount().currency().name(),
                expense.expenseDate(),
                expense.paymentState().name(),
                expense.status().name(),
                expense.expenseType().name(),
                expense.sourceType().name(),
                expense.sourceDebtPaymentId(),
                expense.sourceDebtId(),
                expense.createdAt(),
                expense.updatedAt()
        );
    }
}
