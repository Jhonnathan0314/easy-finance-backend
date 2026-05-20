package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseImportManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final PaymentMethodRepositoryPort paymentMethodRepository = mock(PaymentMethodRepositoryPort.class);
    private final ExpenseImportParserPort parserPort = mock(ExpenseImportParserPort.class);
    private final ExpenseImportTemplateGeneratorPort templateGeneratorPort = mock(ExpenseImportTemplateGeneratorPort.class);
    private final ExpenseImportRepositoryPort repository = mock(ExpenseImportRepositoryPort.class);
    private final CreateImportedExpensePort createImportedExpensePort = mock(CreateImportedExpensePort.class);
    private final ExpenseImportManagementUseCase useCase = new ExpenseImportManagementUseCase(
            currentUserProvider,
            accountAuthorizationService,
            catalogValidationPort,
            categoryRepository,
            paymentMethodRepository,
            parserPort,
            templateGeneratorPort,
            repository,
            createImportedExpensePort,
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
    void confirmCreatesExpensesForValidRowsAndMarksBatchConfirmed() {
        givenCurrentUser();
        ExpenseImportBatch batch = new ExpenseImportBatch(77L, 1L, 10L, "expenses.xlsx", ExpenseImportStatus.PREVIEW, 1, 1, 0, null, null, null, List.of(storedRow(101L)));
        when(repository.findByAccountIdAndIdForUpdate(1L, 77L)).thenReturn(Optional.of(batch));
        when(createImportedExpensePort.createImportedExpense(any())).thenReturn(new ExpenseResponse(500L, 1L, 10L, 20L, 10L, "Lunch", new BigDecimal("120.00"), "COP", LocalDate.of(2026, 5, 1), "PAID", "ACTIVE", "SIMPLE", Instant.now(), Instant.now()));
        when(repository.saveBatch(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.confirm(1L, 77L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(repository).findByAccountIdAndIdForUpdate(1L, 77L);
        verify(repository).updateCreatedExpenseId(1L, 101L, 500L);
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
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-expense-import-template.xlsx");
        assertThat(response.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
        verify(templateGeneratorPort).generate(argThat(data ->
                data.categoryNames().equals(List.of("Food"))
                        && data.paymentMethodNames().equals(List.of("Cash"))));
    }

    @Test
    void generateTemplateUsesReadAuthorizationSoArchivedAccountIsAllowed() {
        givenCurrentUser();
        when(categoryRepository.findActiveExpenseByAccountId(1L)).thenReturn(List.of());
        when(paymentMethodRepository.findActiveByAccountId(1L)).thenReturn(List.of());
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1});

        useCase.generate(1L);

        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    private static PreviewExpenseImportCommand command(String filename) {
        return new PreviewExpenseImportCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private static ExpenseImportRow row(int rowNumber, boolean valid, List<ImportRowError> errors) {
        return new ExpenseImportRow(null, 1L, null, rowNumber, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", null, "Cash", null, ExpensePaymentState.PAID, valid, errors, null, null, null);
    }

    private static ExpenseImportRow storedRow(Long id) {
        return new ExpenseImportRow(id, 1L, 77L, 2, LocalDate.of(2026, 5, 1), "Lunch", Money.cop(new BigDecimal("120.00")), "Food", 10L, "Cash", 20L, ExpensePaymentState.PAID, true, List.of(), null, null, null);
    }
}
