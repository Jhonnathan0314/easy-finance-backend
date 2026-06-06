package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.expenses.application.port.in.CreateDebtPaymentExpensePort;
import com.easyfinance.expenses.application.port.in.CreateImportedExpensePort;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.port.out.ExpenseImportParserPort;
import com.easyfinance.imports.application.port.out.ExpenseImportRepositoryPort;
import com.easyfinance.imports.application.port.out.ExpenseImportTemplateGeneratorPort;
import com.easyfinance.imports.domain.model.ExpenseImportBatch;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.imports.domain.model.ExpenseImportStatus;
import com.easyfinance.imports.domain.model.ImportRowError;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseImportManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final AssignedParticipantValidator assignedParticipantValidator = mock(AssignedParticipantValidator.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final ParticipantLookupPort participantLookupPort = mock(ParticipantLookupPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final PaymentMethodRepositoryPort paymentMethodRepository = mock(PaymentMethodRepositoryPort.class);
    private final DebtRepositoryPort debtRepository = mock(DebtRepositoryPort.class);
    private final ExpenseImportParserPort parserPort = mock(ExpenseImportParserPort.class);
    private final ExpenseImportTemplateGeneratorPort templateGeneratorPort = mock(ExpenseImportTemplateGeneratorPort.class);
    private final ExpenseImportRepositoryPort repository = mock(ExpenseImportRepositoryPort.class);
    private final CreateImportedExpensePort createImportedExpensePort = mock(CreateImportedExpensePort.class);
    private final CreateDebtPaymentExpensePort createDebtPaymentExpensePort = mock(CreateDebtPaymentExpensePort.class);
    private final RegisterDebtPaymentPort registerDebtPaymentPort = mock(RegisterDebtPaymentPort.class);
    private final ExpenseImportManagementUseCase useCase = new ExpenseImportManagementUseCase(
            currentUserProvider,
            accountAuthorizationService,
            assignedParticipantValidator,
            accountParticipantRepository,
            participantLookupPort,
            catalogValidationPort,
            categoryRepository,
            paymentMethodRepository,
            debtRepository,
            parserPort,
            templateGeneratorPort,
            repository,
            createImportedExpensePort,
            createDebtPaymentExpensePort,
            registerDebtPaymentPort,
            5_242_880
    );

    @Test
    void previewValidatesCatalogsAndStoresBatch() {
        givenCurrentUser();
        ExpenseImportRow row = row(2, true, List.of());
        when(parserPort.parse(any(), any())).thenReturn(List.of(row));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
        when(repository.savePreview(any())).thenAnswer(invocation -> {
            ExpenseImportBatch batch = invocation.getArgument(0);
            return new ExpenseImportBatch(99L, batch.accountId(), batch.participantId(), batch.originalFilename(), batch.status(), batch.totalRows(), batch.validRows(), batch.invalidRows(), null, null, null, batch.rows());
        });

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.batchId()).isEqualTo(99L);
        assertThat(response.validRows()).isEqualTo(1);
        assertThat(response.rows()).singleElement().satisfies(item -> {
            assertThat(item.categoryId()).isEqualTo(10L);
            assertThat(item.paymentMethodId()).isEqualTo(20L);
            assertThat(item.valid()).isTrue();
        });
    }

    @Test
    void previewResolvesExplicitParticipantPerRow() {
        givenCurrentUser();
        ExpenseImportRow row = rowWithParticipant(2, "Ana Finance <ana@example.com>");
        when(parserPort.parse(any(), any())).thenReturn(List.of(row));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
        when(repository.savePreview(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.rows()).singleElement().satisfies(item -> {
            assertThat(item.participantLabel()).isEqualTo("Ana Finance <ana@example.com>");
            assertThat(item.participantId()).isEqualTo(11L);
            assertThat(item.valid()).isTrue();
        });
        verify(assignedParticipantValidator).resolveAssignedParticipantId(any(), argThat(id -> id.equals(11L)));
    }

    @Test
    void previewReportsInvalidCatalogRows() {
        givenCurrentUser();
        when(parserPort.parse(any(), any())).thenReturn(List.of(row(2, true, List.of())));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.empty());
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.INACTIVE)));
        when(repository.savePreview(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.rows().getFirst().errors()).extracting("code")
                .contains("CATEGORY_NOT_FOUND", "PAYMENT_METHOD_INACTIVE");
    }

    @Test
    void previewValidatesDebtPaymentAndStoresResolvedDebtFields() {
        givenCurrentUser();
        ExpenseImportRow row = debtPaymentRow(2, true, 30L, new BigDecimal("120.00"));
        when(parserPort.parse(any(), any())).thenReturn(List.of(row));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
        when(debtRepository.findByAccountIdAndId(1L, 30L)).thenReturn(Optional.of(debt(30L, DebtState.ACTIVE, new BigDecimal("500.00"))));
        when(repository.savePreview(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.validRows()).isEqualTo(1);
        assertThat(response.rows()).singleElement().satisfies(item -> {
            assertThat(item.appliesDebtPayment()).isTrue();
            assertThat(item.debtId()).isEqualTo(30L);
            assertThat(item.debtLabel()).isEqualTo("Loan | Saldo: 500.00 | Inicio: 2026-05-01 | MANUAL");
            assertThat(item.debtPaymentType()).isEqualTo("INSTALLMENT");
            assertThat(item.debtPaymentNotes()).isEqualTo("Imported payment");
        });
    }

    @Test
    void previewReportsMissingDebtFromHiddenMapping() {
        givenCurrentUser();
        ExpenseImportRow row = debtPaymentRow(2, true, null, new BigDecimal("120.00"));
        when(parserPort.parse(any(), any())).thenReturn(List.of(row));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
        when(repository.savePreview(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.rows().getFirst().errors()).extracting("code")
                .contains("IMPORT_DEBT_NOT_FOUND");
    }

    @Test
    void previewReportsInactiveDebtAndOverpayment() {
        givenCurrentUser();
        ExpenseImportRow row = debtPaymentRow(2, true, 30L, new BigDecimal("600.00"));
        when(parserPort.parse(any(), any())).thenReturn(List.of(row));
        when(catalogValidationPort.findCategoryForValidation(1L, "food")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findPaymentMethodForValidation(1L, "cash")).thenReturn(Optional.of(new PaymentMethodValidationView(20L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));
        when(debtRepository.findByAccountIdAndId(1L, 30L)).thenReturn(Optional.of(debt(30L, DebtState.PAID, new BigDecimal("500.00"))));
        when(repository.savePreview(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.preview(command("expenses.xlsx"));

        assertThat(response.invalidRows()).isEqualTo(1);
        assertThat(response.rows().getFirst().errors()).extracting("code")
                .contains("IMPORT_DEBT_NOT_ACTIVE", "IMPORT_DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE");
    }

    @Test
    void confirmCreatesExpensesForValidRowsAndMarksBatchConfirmed() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedRowWithParticipant(101L, 11L)));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(createImportedExpensePort.createImportedExpense(any())).thenReturn(new ExpenseResponse(500L, 1L, 10L, 20L, 10L, "Lunch", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "PAID", "ACTIVE", "SIMPLE", Instant.now(), Instant.now()));
        when(repository.saveBatch(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.confirm(1L, 77L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(repository).findByAccountIdAndIdForUpdate(1L, 77L);
        verify(createImportedExpensePort).createImportedExpense(argThat(command -> command.participantId().equals(11L)));
        verify(repository).updateCreatedExpenseId(1L, 101L, 500L);
        verify(registerDebtPaymentPort, never()).registerDebtPayment(any());
    }

    @Test
    void confirmCreatesDebtPaymentForDebtRowsAndStoresTraceId() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedDebtPaymentRow(101L, "  Imported payment  ")));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(registerDebtPaymentPort.registerDebtPayment(any())).thenReturn(debtPaymentResponse(900L));
        when(createDebtPaymentExpensePort.createDebtPaymentExpense(any())).thenReturn(new ExpenseResponse(500L, 1L, 10L, 20L, 10L, "Lunch", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "PAID", "ACTIVE", "SIMPLE", "DEBT_PAYMENT", 900L, Instant.now(), Instant.now()));
        when(repository.saveBatch(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.confirm(1L, 77L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(registerDebtPaymentPort).registerDebtPayment(argThat(command ->
                command.accountId().equals(1L)
                        && command.debtId().equals(30L)
                        && command.paymentType() == DebtPaymentType.INSTALLMENT
                        && command.amount().amount().compareTo(new BigDecimal("120.00")) == 0
                        && command.paymentDate().equals(LocalDate.of(2026, 5, 1))
                        && command.participantId().equals(10L)
                        && command.notes().equals("Imported payment")
                        && !command.shouldCreateExpense()
        ));
        verify(repository).updateCreatedExpenseId(1L, 101L, 500L);
        verify(repository).updateCreatedDebtPaymentId(1L, 101L, 900L);
    }

    @Test
    void confirmUsesDefaultDebtPaymentNotesWhenBlank() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedDebtPaymentRow(101L, " ")));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(registerDebtPaymentPort.registerDebtPayment(any())).thenReturn(debtPaymentResponse(900L));
        when(createDebtPaymentExpensePort.createDebtPaymentExpense(any())).thenReturn(new ExpenseResponse(500L, 1L, 10L, 20L, 10L, "Lunch", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "PAID", "ACTIVE", "SIMPLE", "DEBT_PAYMENT", 900L, Instant.now(), Instant.now()));
        when(repository.saveBatch(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.confirm(1L, 77L);

        verify(registerDebtPaymentPort).registerDebtPayment(argThat(command ->
                command.notes().equals("Pago registrado desde importacion de gastos")
        ));
    }

    @Test
    void debtPaymentFailureDuringConfirmIsWrappedAsImportConfirmationFailure() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedDebtPaymentRow(101L, null)));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(createImportedExpensePort.createImportedExpense(any())).thenReturn(new ExpenseResponse(500L, 1L, 10L, 20L, 10L, "Lunch", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "PAID", "ACTIVE", "SIMPLE", Instant.now(), Instant.now()));
        when(registerDebtPaymentPort.registerDebtPayment(any())).thenThrow(new BusinessRuleViolationException("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE", "Debt payment exceeds remaining balance."));

        assertThatThrownBy(() -> useCase.confirm(1L, 77L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("IMPORT_CONFIRMATION_FAILED"));
    }

    @Test
    void confirmedBatchCannotBeConfirmedAgain() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.CONFIRMED, 1, 1, 0, Instant.now(), null, null, List.of(storedRow(101L)));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> useCase.confirm(1L, 77L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("IMPORT_ALREADY_CONFIRMED"));
    }

    @Test
    void unexpectedRuntimeFailureDuringConfirmIsNotMaskedAsBusinessError() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedRow(101L)));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(createImportedExpensePort.createImportedExpense(any())).thenThrow(new IllegalStateException("Database is down."));

        assertThatThrownBy(() -> useCase.confirm(1L, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database is down.");
    }

    @Test
    void invalidFileTypeFails() {
        givenCurrentUser();

        assertThatThrownBy(() -> useCase.preview(command("expenses.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateAllowsActiveMemberAndUsesActiveCatalogValues() {
        givenCurrentUser();
        when(categoryRepository.findActiveExpenseByAccountId(1L)).thenReturn(List.of(
                Category.restore(10L, 1L, "Food", null, CategoryType.EXPENSE, CatalogStatus.ACTIVE, Instant.now(), Instant.now())
        ));
        when(paymentMethodRepository.findActiveByAccountId(1L)).thenReturn(List.of(
                PaymentMethod.restore(20L, 1L, "Cash", null, PaymentMethodType.CASH, CatalogStatus.ACTIVE, Instant.now(), Instant.now())
        ));
        when(debtRepository.findActiveByAccountId(1L)).thenReturn(List.of(
                debt(30L, DebtState.ACTIVE, new BigDecimal("500.00"))
        ));
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-expense-import-template.xlsx");
        assertThat(response.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
        verify(templateGeneratorPort).generate(argThat(data ->
                data.categoryNames().equals(List.of("Food"))
                        && data.paymentMethodNames().equals(List.of("Cash"))
                        && data.debtOptions().size() == 1
                        && data.debtOptions().getFirst().debtId().equals(30L)
                        && data.debtOptions().getFirst().label().equals("Loan | Saldo: 500.00 | Inicio: 2026-05-01 | MANUAL")));
    }

    @Test
    void generateTemplateUsesReadAuthorizationSoArchivedAccountIsAllowed() {
        givenCurrentUser();
        when(categoryRepository.findActiveExpenseByAccountId(1L)).thenReturn(List.of());
        when(paymentMethodRepository.findActiveByAccountId(1L)).thenReturn(List.of());
        when(debtRepository.findActiveByAccountId(1L)).thenReturn(List.of());
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1});

        useCase.generate(1L);

        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
        Account account = Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now());
        AccountParticipant actor = AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), null, null);
        AccountAccess access = new AccountAccess(account, actor);
        when(accountAuthorizationService.requireActiveMemberForActiveAccount(1L, 10L)).thenReturn(access);
        when(accountParticipantRepository.findByAccountId(1L)).thenReturn(List.of(
                actor,
                AccountParticipant.restore(2L, 1L, 11L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)
        ));
        when(participantLookupPort.findByParticipantIds(any())).thenReturn(Map.of(
                10L, new ParticipantInfo(10L, 100L, "user@example.com", "Current User", true),
                11L, new ParticipantInfo(11L, 101L, "ana@example.com", "Ana Finance", true)
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), any())).thenAnswer(invocation -> {
            Long requested = invocation.getArgument(1);
            return requested == null ? 10L : requested;
        });
    }

    private static PreviewExpenseImportCommand command(String filename) {
        return new PreviewExpenseImportCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private static ExpenseImportRow row(int rowNumber, boolean valid, List<ImportRowError> errors) {
        return new ExpenseImportRow(null, 1L, null, rowNumber, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", null, "Cash", null, ExpensePaymentState.PAID, false, null, null, null, null, valid, errors, null, null, null, null);
    }

    private static ExpenseImportRow rowWithParticipant(int rowNumber, String participantLabel) {
        return new ExpenseImportRow(null, 1L, null, rowNumber, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", null, "Cash", null, ExpensePaymentState.PAID, participantLabel, null, false, null, null, null, null, true, List.of(), null, null, null, null);
    }

    private static ExpenseImportRow debtPaymentRow(int rowNumber, boolean valid, Long debtId, BigDecimal amount) {
        return new ExpenseImportRow(null, 1L, null, rowNumber, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(amount), "Food", null, "Cash", null, ExpensePaymentState.PAID, true, debtId, "Loan | Saldo: 500.00 | Inicio: 2026-05-01 | MANUAL", DebtPaymentType.INSTALLMENT, "Imported payment", valid, List.of(), null, null, null, null);
    }

    private static ExpenseImportRow storedRow(Long id) {
        return new ExpenseImportRow(id, 1L, 77L, 2, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", 10L, "Cash", 20L, ExpensePaymentState.PAID, false, null, null, null, null, true, List.of(), null, null, null, null);
    }

    private static ExpenseImportRow storedRowWithParticipant(Long id, Long participantId) {
        return new ExpenseImportRow(id, 1L, 77L, 2, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", 10L, "Cash", 20L, ExpensePaymentState.PAID, "Ana Finance <ana@example.com>", participantId, false, null, null, null, null, true, List.of(), null, null, null, null);
    }

    private static ExpenseImportRow storedDebtPaymentRow(Long id, String notes) {
        return new ExpenseImportRow(id, 1L, 77L, 2, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", 10L, "Cash", 20L, ExpensePaymentState.PAID, true, 30L, "Loan | Saldo: 500.00 | Inicio: 2026-05-01 | MANUAL", DebtPaymentType.INSTALLMENT, notes, true, List.of(), null, null, null, null);
    }

    private static RegisterDebtPaymentResponse debtPaymentResponse(Long paymentId) {
        return new RegisterDebtPaymentResponse(
                new DebtPaymentResponse(paymentId, 1L, 30L, 10L, "INSTALLMENT", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "Imported payment", "ACTIVE", Instant.now(), Instant.now()),
                new DebtResponse(30L, 1L, 10L, null, "MANUAL", "Loan", null, new BigDecimal("1000.00"), new BigDecimal("1000.00"), "COP", new BigDecimal("380.00"), "COP", null, null, null, LocalDate.of(2026, 5, 1), null, "ACTIVE", null, Instant.now(), Instant.now())
        );
    }

    private static Debt debt(Long id, DebtState state, BigDecimal remainingBalance) {
        return Debt.restore(
                id,
                1L,
                10L,
                null,
                DebtSourceType.MANUAL,
                "Loan",
                null,
                Money.cop(new BigDecimal("1000.00")),
                Money.cop(new BigDecimal("1000.00")),
                Money.cop(remainingBalance),
                null,
                null,
                LocalDate.of(2026, 5, 1),
                null,
                state,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
