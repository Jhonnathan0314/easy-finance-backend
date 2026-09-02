package com.easyfinance.expenses.application.usecase;

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
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.port.in.CreateInstallmentExpenseDebtPort;
import com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand;
import com.easyfinance.expenses.application.command.CreateExpenseCommand;
import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.command.DuplicateExpenseCommand;
import com.easyfinance.expenses.application.command.UpdateExpenseCommand;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseSourceType;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
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

class ExpenseManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final CreateInstallmentExpenseDebtPort createInstallmentExpenseDebtPort = mock(CreateInstallmentExpenseDebtPort.class);
    private final ExpenseRepositoryPort expenseRepository = mock(ExpenseRepositoryPort.class);
    private final AccountAuthorizationService accountAuthorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final AssignedParticipantValidator assignedParticipantValidator = new AssignedParticipantValidator(accountParticipantRepository);
    private final ExpenseManagementUseCase useCase = new ExpenseManagementUseCase(currentUserProvider, accountAuthorizationService, catalogValidationPort, assignedParticipantValidator, createInstallmentExpenseDebtPort, expenseRepository);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void createExpenseAsMemberWorks() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createExpense(createCommand());

        assertThat(response.participantId()).isEqualTo(10L);
        assertThat(response.paymentState()).isEqualTo("PAID");
        assertThat(response.expenseType()).isEqualTo("SIMPLE");
        assertThat(response.sourceType()).isEqualTo("MANUAL");
        assertThat(response.sourceDebtPaymentId()).isNull();
    }

    @Test
    void adminCreatesExpenseAssignedToAnotherActiveParticipant() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenAssignedParticipant(20L, AccountParticipantStatus.ACTIVE);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createExpense(createCommand(20L));

        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void memberCannotCreateExpenseAssignedToAnotherParticipant() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);

        assertThatThrownBy(() -> useCase.createExpense(createCommand(20L)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ALLOWED"));

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void createImportedExpenseMarksSourceTypeImport() {
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createImportedExpense(new com.easyfinance.expenses.application.command.CreateImportedExpenseCommand(
                1L,
                2L,
                3L,
                10L,
                "Imported lunch",
                Money.cop(new BigDecimal("12000")),
                LocalDate.of(2026, 5, 11),
                ExpensePaymentState.PAID
        ));

        assertThat(response.sourceType()).isEqualTo("IMPORT");
        assertThat(response.sourceDebtPaymentId()).isNull();
    }

    @Test
    void createDebtPaymentExpenseMarksDebtPaymentSource() {
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createDebtPaymentExpense(new CreateDebtPaymentExpenseCommand(
                1L,
                2L,
                3L,
                10L,
                30L,
                50L,
                "Debt payment",
                Money.cop(new BigDecimal("40000")),
                LocalDate.of(2026, 5, 11)
        ));

        assertThat(response.paymentState()).isEqualTo("PAID");
        assertThat(response.expenseType()).isEqualTo("SIMPLE");
        assertThat(response.sourceType()).isEqualTo("DEBT_PAYMENT");
        assertThat(response.sourceDebtPaymentId()).isEqualTo(50L);
        assertThat(response.sourceDebtId()).isEqualTo(30L);
    }

    @Test
    void createExpenseInArchivedAccountFails() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void createExpenseWithIncomeCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L))
                .thenReturn(Optional.of(new CategoryValidationView(2L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_CATEGORY_INVALID_TYPE"));
    }

    @Test
    void createExpenseWithMissingCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_CATEGORY_NOT_FOUND"));
    }

    @Test
    void createExpenseWithInactiveCategoryFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_CATEGORY_INACTIVE"));
    }

    @Test
    void createExpenseWithMissingPaymentMethodFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_PAYMENT_METHOD_NOT_FOUND"));
    }

    @Test
    void createInstallmentExpenseCreatesExpenseAndDebt() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                1L,
                null,
                2L,
                3L,
                "Laptop",
                Money.cop(new BigDecimal("1200000")),
                LocalDate.of(2026, 5, 11),
                6,
                Money.cop(new BigDecimal("200000")),
                LocalDate.of(2026, 6, 1),
                "Laptop debt",
                "No payments yet"
        ));

        assertThat(response.expenseType()).isEqualTo("INSTALLMENT");
        assertThat(response.paymentState()).isEqualTo("PENDING");
        ArgumentCaptor<CreateInstallmentExpenseDebtCommand> commandCaptor = ArgumentCaptor.forClass(CreateInstallmentExpenseDebtCommand.class);
        verify(createInstallmentExpenseDebtPort).createInstallmentExpenseDebt(commandCaptor.capture());
        assertThat(commandCaptor.getValue().totalAmount().amount()).isEqualByComparingTo("1200000.00");
        assertThat(commandCaptor.getValue().participantId()).isEqualTo(10L);
    }

    @Test
    void createInstallmentExpenseAssignedToAnotherParticipantPropagatesToDebt() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenAssignedParticipant(20L, AccountParticipantStatus.ACTIVE);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createInstallmentExpense(installmentCommand(20L));

        ArgumentCaptor<CreateInstallmentExpenseDebtCommand> commandCaptor = ArgumentCaptor.forClass(CreateInstallmentExpenseDebtCommand.class);
        verify(createInstallmentExpenseDebtPort).createInstallmentExpenseDebt(commandCaptor.capture());
        assertThat(response.participantId()).isEqualTo(20L);
        assertThat(commandCaptor.getValue().participantId()).isEqualTo(20L);
    }

    @Test
    void createInstallmentExpenseWithFinancingCostsCreatesDebtForFinancedTotal() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        var response = useCase.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                1L,
                null,
                2L,
                3L,
                "Advance",
                Money.cop(new BigDecimal("1000000")),
                LocalDate.of(2026, 5, 11),
                12,
                Money.cop(new BigDecimal("100000")),
                LocalDate.of(2026, 6, 1),
                "Advance debt",
                "Includes financing costs"
        ));

        ArgumentCaptor<CreateInstallmentExpenseDebtCommand> commandCaptor = ArgumentCaptor.forClass(CreateInstallmentExpenseDebtCommand.class);
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(expenseCaptor.capture());
        verify(createInstallmentExpenseDebtPort).createInstallmentExpenseDebt(commandCaptor.capture());
        assertThat(response.amount()).isEqualByComparingTo("1000000.00");
        assertThat(expenseCaptor.getValue().amount().amount()).isEqualByComparingTo("1000000.00");
        assertThat(commandCaptor.getValue().totalAmount().amount()).isEqualByComparingTo("1000000.00");
    }

    @Test
    void createInstallmentExpenseWithFinancedTotalLowerThanOriginalAmountFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();

        assertThatThrownBy(() -> useCase.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                1L,
                null,
                2L,
                3L,
                "Advance",
                Money.cop(new BigDecimal("1000000")),
                LocalDate.of(2026, 5, 11),
                9,
                Money.cop(new BigDecimal("100000")),
                LocalDate.of(2026, 6, 1),
                "Advance debt",
                null
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INSTALLMENT_FINANCED_TOTAL_INVALID"));
        verify(expenseRepository, never()).save(any());
        verify(createInstallmentExpenseDebtPort, never()).createInstallmentExpenseDebt(any());
    }

    @Test
    void createInstallmentExpenseWithInvalidCountFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();

        assertThatThrownBy(() -> useCase.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                1L,
                null,
                2L,
                3L,
                "Laptop",
                Money.cop(new BigDecimal("1200000")),
                LocalDate.of(2026, 5, 11),
                0,
                Money.cop(new BigDecimal("200000")),
                LocalDate.of(2026, 6, 1),
                null,
                null
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_INSTALLMENT_COUNT_INVALID"));
    }

    @Test
    void createInstallmentExpenseWithExpectedDebtFailureReturnsStableError() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
        doThrow(new BusinessRuleViolationException("DEBT_ORIGIN_EXPENSE_INVALID_TYPE", "Invalid origin."))
                .when(createInstallmentExpenseDebtPort).createInstallmentExpenseDebt(any());

        assertThatThrownBy(() -> useCase.createInstallmentExpense(installmentCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INSTALLMENT_EXPENSE_DEBT_CREATION_FAILED"));
    }

    @Test
    void createInstallmentExpenseWithUnexpectedDebtFailurePropagates() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
        doThrow(new IllegalStateException("database is unhappy"))
                .when(createInstallmentExpenseDebtPort).createInstallmentExpenseDebt(any());

        assertThatThrownBy(() -> useCase.createInstallmentExpense(installmentCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database is unhappy");
    }

    @Test
    void createExpenseWithInactivePaymentMethodFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L))
                .thenReturn(Optional.of(new PaymentMethodValidationView(3L, 1L, PaymentMethodType.CASH, CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.createExpense(createCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_PAYMENT_METHOD_INACTIVE"));
    }

    @Test
    void updateByOwnerWorks() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 10L, ExpenseStatus.ACTIVE)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateExpense(updateCommand());

        assertThat(response.description()).isEqualTo("Dinner");
        assertThat(response.participantId()).isEqualTo(10L);
    }

    @Test
    void updateByAnotherMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateExpense(updateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_UPDATE_NOT_ALLOWED"));
    }

    @Test
    void updateByAdminWorksForAnotherParticipantExpense() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateExpense(updateCommand());

        assertThat(response.description()).isEqualTo("Dinner");
        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void adminReassignsExpenseToAnotherActiveParticipant() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenAssignedParticipant(30L, AccountParticipantStatus.ACTIVE);
        givenValidCatalogs();
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.updateExpense(updateCommand(30L));

        assertThat(response.participantId()).isEqualTo(30L);
    }

    @Test
    void memberCannotReassignExpenseToAnotherParticipant() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 10L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateExpense(updateCommand(20L)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ASSIGNED_PARTICIPANT_NOT_ALLOWED"));

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateInstallmentExpenseFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(installmentExpense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateExpense(updateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INSTALLMENT_EXPENSE_UPDATE_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateDebtPaymentExpenseFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(debtPaymentExpense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateExpense(updateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_DEBT_PAYMENT_UPDATE_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateCancelledExpenseFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.CANCELLED)));

        assertThatThrownBy(() -> useCase.updateExpense(updateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_ALREADY_CANCELLED"));
    }

    @Test
    void cancelByAnotherMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.cancelExpense(1L, 5L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_CANCEL_NOT_ALLOWED"));
    }

    @Test
    void cancelSimpleExpenseByOwnerWorks() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 10L, ExpenseStatus.ACTIVE)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelExpense(1L, 5L);

        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void cancelInstallmentExpenseFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(installmentExpense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.cancelExpense(1L, 5L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INSTALLMENT_EXPENSE_CANCEL_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void cancelDebtPaymentExpenseFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(debtPaymentExpense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.cancelExpense(1L, 5L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_DEBT_PAYMENT_CANCEL_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void ownerDuplicatesSimpleActiveExpenseWithOverrides() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        Expense source = expense(5L, 10L, ExpenseStatus.ACTIVE);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        var response = useCase.duplicateExpense(new DuplicateExpenseCommand(
                1L,
                5L,
                LocalDate.of(2026, 6, 15),
                Money.cop(new BigDecimal("85000")),
                "Mercado junio",
                ExpensePaymentState.PAID
        ));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        verify(createInstallmentExpenseDebtPort, never()).createInstallmentExpenseDebt(any());
        Expense duplicate = captor.getValue();
        assertThat(response.id()).isEqualTo(9L);
        assertThat(duplicate.id()).isNull();
        assertThat(duplicate.accountId()).isEqualTo(source.accountId());
        assertThat(duplicate.categoryId()).isEqualTo(source.categoryId());
        assertThat(duplicate.paymentMethodId()).isEqualTo(source.paymentMethodId());
        assertThat(duplicate.participantId()).isEqualTo(source.participantId());
        assertThat(duplicate.description()).isEqualTo("Mercado junio");
        assertThat(duplicate.amount().amount()).isEqualByComparingTo("85000");
        assertThat(duplicate.expenseDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(duplicate.paymentState()).isEqualTo(ExpensePaymentState.PAID);
        assertThat(duplicate.status()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(duplicate.expenseType()).isEqualTo(ExpenseType.SIMPLE);
        assertThat(source.id()).isEqualTo(5L);
        assertThat(source.description()).isEqualTo("Lunch");
    }

    @Test
    void duplicateExpenseUsesSourceValuesWhenOverridesAreMissing() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        Expense source = expense(5L, 10L, ExpenseStatus.ACTIVE);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        useCase.duplicateExpense(new DuplicateExpenseCommand(1L, 5L, LocalDate.of(2026, 6, 15), null, "   ", null));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense duplicate = captor.getValue();
        assertThat(duplicate.description()).isEqualTo(source.description());
        assertThat(duplicate.amount().amount()).isEqualByComparingTo(source.amount().amount());
        assertThat(duplicate.paymentState()).isEqualTo(source.paymentState());
    }

    @Test
    void duplicatingDebtPaymentExpenseResetsSourceToManual() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        Expense source = debtPaymentExpense(5L, 20L, ExpenseStatus.ACTIVE);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        var response = useCase.duplicateExpense(duplicateCommand());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense duplicate = captor.getValue();
        assertThat(response.sourceType()).isEqualTo("MANUAL");
        assertThat(response.sourceDebtPaymentId()).isNull();
        assertThat(response.sourceDebtId()).isNull();
        assertThat(duplicate.sourceType()).isEqualTo(ExpenseSourceType.MANUAL);
        assertThat(duplicate.sourceDebtPaymentId()).isNull();
        assertThat(duplicate.sourceDebtId()).isNull();
    }

    @Test
    void duplicateOfDebtPaymentExpenseCanBeUpdatedAndCancelledAfterward() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        Expense source = debtPaymentExpense(5L, 20L, ExpenseStatus.ACTIVE);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(source));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        useCase.duplicateExpense(duplicateCommand());

        Expense duplicated = expense(9L, 20L, ExpenseStatus.ACTIVE);
        when(expenseRepository.findByAccountIdAndId(1L, 9L)).thenReturn(Optional.of(duplicated));

        var updateResponse = useCase.updateExpense(new UpdateExpenseCommand(1L, 9L, null, 2L, 3L, "Dinner", Money.cop(new BigDecimal("15000")), LocalDate.now(), ExpensePaymentState.PAID));
        assertThat(updateResponse.description()).isEqualTo("Dinner");

        useCase.cancelExpense(1L, 9L);
    }

    @Test
    void adminDuplicatesAnotherParticipantExpense() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        givenValidCatalogs();
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> persistedWithId(invocation.getArgument(0), 9L));

        var response = useCase.duplicateExpense(duplicateCommand());

        assertThat(response.participantId()).isEqualTo(20L);
    }

    @Test
    void anotherMemberCannotDuplicateExpense() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_DUPLICATE_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void archivedAccountBlocksDuplicateExpense() {
        givenMemberAccess(AccountStatus.ARCHIVED, 10L);

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void duplicateExpenseReturnsNotFoundForMissingSource() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_NOT_FOUND"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void cancelledExpenseCannotBeDuplicated() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.CANCELLED)));

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_DUPLICATE_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void installmentExpenseCannotBeDuplicated() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(installmentExpense(5L, 20L, ExpenseStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_DUPLICATE_NOT_ALLOWED"));
        verify(expenseRepository, never()).save(any());
        verify(createInstallmentExpenseDebtPort, never()).createInstallmentExpenseDebt(any());
    }

    @Test
    void duplicateExpenseWithInactiveCategoryFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_CATEGORY_INACTIVE"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void duplicateExpenseWithInactivePaymentMethodFails() {
        givenAdminAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 5L)).thenReturn(Optional.of(expense(5L, 20L, ExpenseStatus.ACTIVE)));
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L))
                .thenReturn(Optional.of(new PaymentMethodValidationView(3L, 1L, PaymentMethodType.CASH, CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.duplicateExpense(duplicateCommand()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_PAYMENT_METHOD_INACTIVE"));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void getExpenseOutsideAccountReturnsNotFound() {
        givenMemberAccess(AccountStatus.ACTIVE, 10L);
        when(expenseRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getExpense(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("EXPENSE_NOT_FOUND"));
    }

    private void givenValidCatalogs() {
        when(catalogValidationPort.findCategoryForValidation(1L, 2L)).thenReturn(Optional.of(expenseCategory(CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L)).thenReturn(Optional.of(new PaymentMethodValidationView(3L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
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

    private static CreateExpenseCommand createCommand() {
        return createCommand(null);
    }

    private static CreateExpenseCommand createCommand(Long participantId) {
        return new CreateExpenseCommand(1L, participantId, 2L, 3L, "Lunch", Money.cop(new BigDecimal("12000")), LocalDate.now(), null);
    }

    private static UpdateExpenseCommand updateCommand() {
        return updateCommand(null);
    }

    private static UpdateExpenseCommand updateCommand(Long participantId) {
        return new UpdateExpenseCommand(1L, 5L, participantId, 2L, 3L, "Dinner", Money.cop(new BigDecimal("15000")), LocalDate.now(), ExpensePaymentState.PAID);
    }

    private static DuplicateExpenseCommand duplicateCommand() {
        return new DuplicateExpenseCommand(1L, 5L, LocalDate.of(2026, 6, 15), null, null, null);
    }

    private static CreateInstallmentExpenseCommand installmentCommand() {
        return installmentCommand(null);
    }

    private static CreateInstallmentExpenseCommand installmentCommand(Long participantId) {
        return new CreateInstallmentExpenseCommand(
                1L,
                participantId,
                2L,
                3L,
                "Laptop",
                Money.cop(new BigDecimal("1200000")),
                LocalDate.of(2026, 5, 11),
                6,
                Money.cop(new BigDecimal("200000")),
                LocalDate.of(2026, 6, 1),
                "Laptop debt",
                "No payments yet"
        );
    }

    private static CategoryValidationView expenseCategory(CatalogStatus status) {
        return new CategoryValidationView(2L, 1L, CategoryType.EXPENSE, status);
    }

    private static Expense persisted(Expense expense) {
        return Expense.restore(5L, expense.accountId(), expense.categoryId(), expense.paymentMethodId(), expense.participantId(), expense.description(), expense.amount(), expense.expenseDate(), expense.paymentState(), expense.status(), expense.expenseType(), expense.sourceType(), expense.sourceDebtPaymentId(), expense.sourceDebtId(), Instant.now(), Instant.now());
    }

    private static Expense persistedWithId(Expense expense, Long id) {
        return Expense.restore(id, expense.accountId(), expense.categoryId(), expense.paymentMethodId(), expense.participantId(), expense.description(), expense.amount(), expense.expenseDate(), expense.paymentState(), expense.status(), expense.expenseType(), expense.sourceType(), expense.sourceDebtPaymentId(), expense.sourceDebtId(), Instant.now(), Instant.now());
    }

    private static Expense expense(Long id, Long participantId, ExpenseStatus status) {
        return Expense.restore(id, 1L, 2L, 3L, participantId, "Lunch", Money.cop(new BigDecimal("12000")), LocalDate.now(), ExpensePaymentState.PAID, status, ExpenseType.SIMPLE, Instant.now(), Instant.now());
    }

    private static Expense installmentExpense(Long id, Long participantId, ExpenseStatus status) {
        return Expense.restore(id, 1L, 2L, 3L, participantId, "Laptop", Money.cop(new BigDecimal("1200000")), LocalDate.now(), ExpensePaymentState.PENDING, status, ExpenseType.INSTALLMENT, Instant.now(), Instant.now());
    }

    private static Expense debtPaymentExpense(Long id, Long participantId, ExpenseStatus status) {
        return Expense.restore(id, 1L, 2L, 3L, participantId, "Debt payment", Money.cop(new BigDecimal("40000")), LocalDate.now(), ExpensePaymentState.PAID, status, ExpenseType.SIMPLE, ExpenseSourceType.DEBT_PAYMENT, 50L, 30L, Instant.now(), Instant.now());
    }
}
