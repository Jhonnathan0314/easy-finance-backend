package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.port.out.CategoryImportParserPort;
import com.easyfinance.imports.application.port.out.CategoryImportTemplateGeneratorPort;
import com.easyfinance.imports.application.validation.CategoryImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final CategoryImportParserPort parserPort = mock(CategoryImportParserPort.class);
    private final CategoryImportTemplateGeneratorPort templateGeneratorPort = mock(CategoryImportTemplateGeneratorPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final CreateCategoryPort createCategoryPort = mock(CreateCategoryPort.class);
    private final CategoryImportUseCase useCase = new CategoryImportUseCase(
            currentUserProvider,
            accountAuthorizationService,
            parserPort,
            templateGeneratorPort,
            categoryRepository,
            createCategoryPort,
            5_242_880
    );

    @Test
    void importCreatesAllWhenRowsAreValid() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new CategoryImportParsedRow(2, "Mercado", "Compras del hogar", CategoryType.EXPENSE, List.of()),
                new CategoryImportParsedRow(3, "Nomina", null, CategoryType.INCOME, List.of())
        ));
        when(createCategoryPort.createCategory(any()))
                .thenReturn(category(101L, "Mercado", "EXPENSE"))
                .thenReturn(category(102L, "Nomina", "INCOME"));

        var response = useCase.importCategories(command("categories.xlsx"));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.rows()).allMatch(row -> row.valid() && row.createdCategoryId() != null);
        ArgumentCaptor<com.easyfinance.catalogs.application.command.CreateCategoryCommand> captor =
                ArgumentCaptor.forClass(com.easyfinance.catalogs.application.command.CreateCategoryCommand.class);
        verify(createCategoryPort, org.mockito.Mockito.times(2)).createCategory(captor.capture());
        assertThat(captor.getAllValues().getFirst().description()).isEqualTo("Compras del hogar");
        assertThat(captor.getAllValues().get(1).description()).isNull();
    }

    @Test
    void importDoesNotCreateAnyWhenOneRowIsInvalid() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new CategoryImportParsedRow(2, "Mercado", null, CategoryType.EXPENSE, List.of()),
                new CategoryImportParsedRow(3, null, null, null, List.of("Nombre requerido"))
        ));

        var response = useCase.importCategories(command("categories.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows()).anyMatch(row -> !row.valid());
        verify(createCategoryPort, never()).createCategory(any());
    }

    @Test
    void importDetectsDuplicateActiveCategoryInDatabase() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new CategoryImportParsedRow(2, "Mercado", null, CategoryType.EXPENSE, List.of())
        ));
        when(categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedName(1L, CategoryType.EXPENSE, "mercado"))
                .thenReturn(true);

        var response = useCase.importCategories(command("categories.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().errors()).contains("Categoria ya existe");
        verify(createCategoryPort, never()).createCategory(any());
    }

    @Test
    void previewReturnsParsedRowDataAndDoesNotCreate() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new CategoryImportParsedRow(2, "Mercado", "Compras del hogar", CategoryType.EXPENSE, List.of())
        ));

        var response = useCase.previewCategories(command("categories.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().name()).isEqualTo("Mercado");
        assertThat(response.rows().getFirst().description()).isEqualTo("Compras del hogar");
        assertThat(response.rows().getFirst().type()).isEqualTo(CategoryType.EXPENSE);
        verify(createCategoryPort, never()).createCategory(any());
    }

    @Test
    void importAllowsSameNameWhenOnlyInactiveExists() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new CategoryImportParsedRow(2, "Mercado", null, CategoryType.EXPENSE, List.of())
        ));
        when(categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedName(1L, CategoryType.EXPENSE, "mercado"))
                .thenReturn(false);
        when(createCategoryPort.createCategory(any())).thenReturn(category(101L, "Mercado", "EXPENSE"));

        var response = useCase.importCategories(command("categories.xlsx"));

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.rows().getFirst().createdCategoryId()).isEqualTo(101L);
    }

    @Test
    void importFailsForInvalidExtension() {
        givenCurrentUser();

        assertThatThrownBy(() -> useCase.importCategories(command("categories.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateReturnsWorkbook() {
        givenCurrentUser();
        when(templateGeneratorPort.generate()).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-category-import-template.xlsx");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveAdminForActiveAccount(1L, 10L);
    }

    @Test
    void importRequiresAdminRole() {
        givenCurrentUser();
        when(accountAuthorizationService.requireActiveAdminForActiveAccount(1L, 10L))
                .thenThrow(new ForbiddenOperationException("ACCOUNT_ADMIN_REQUIRED", "Account admin role is required."));

        assertThatThrownBy(() -> useCase.importCategories(command("categories.xlsx")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void importRequiresActiveAccount() {
        givenCurrentUser();
        when(accountAuthorizationService.requireActiveAdminForActiveAccount(1L, 10L))
                .thenThrow(new ForbiddenOperationException("ACCOUNT_NOT_ACTIVE", "Account is not active."));

        assertThatThrownBy(() -> useCase.importCategories(command("categories.xlsx")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "admin@example.com", Set.of("USER"), true)));
    }

    private static ImportCategoryCommand command(String filename) {
        return new ImportCategoryCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private static CategoryResponse category(Long id, String name, String type) {
        return new CategoryResponse(id, 1L, name, null, type, "ACTIVE", Instant.now(), Instant.now());
    }
}
