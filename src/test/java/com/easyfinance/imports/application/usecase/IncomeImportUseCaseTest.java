package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.port.out.IncomeImportParserPort;
import com.easyfinance.imports.application.port.out.IncomeImportTemplateGeneratorPort;
import com.easyfinance.imports.application.validation.IncomeImportParsedRow;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.income.application.response.IncomeResponse;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncomeImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final IncomeImportParserPort parserPort = mock(IncomeImportParserPort.class);
    private final IncomeImportTemplateGeneratorPort templateGeneratorPort = mock(IncomeImportTemplateGeneratorPort.class);
    private final CreateIncomePort createIncomePort = mock(CreateIncomePort.class);
    private final IncomeImportUseCase useCase = new IncomeImportUseCase(
            currentUserProvider,
            accountAuthorizationService,
            catalogValidationPort,
            categoryRepository,
            parserPort,
            templateGeneratorPort,
            createIncomePort,
            5_242_880
    );

    @Test
    void importCreatesAllWhenRowsAreValid() {
        givenCurrentUser();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", new BigDecimal("5000000"), List.of()),
                new IncomeImportParsedRow(3, LocalDate.of(2026, 5, 12), "Freelance", "Servicios", new BigDecimal("700000"), List.of())
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "salario")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findCategoryForValidation(1L, "servicios")).thenReturn(Optional.of(new CategoryValidationView(11L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));
        when(createIncomePort.createIncome(any()))
                .thenReturn(new IncomeResponse(101L, 1L, 10L, 10L, "Nomina", new BigDecimal("5000000"), "COP", LocalDate.of(2026, 5, 10), "ACTIVE", Instant.now(), Instant.now()))
                .thenReturn(new IncomeResponse(102L, 1L, 11L, 10L, "Freelance", new BigDecimal("700000"), "COP", LocalDate.of(2026, 5, 12), "ACTIVE", Instant.now(), Instant.now()));

        var response = useCase.importIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.rows()).allMatch(row -> row.valid() && row.createdIncomeId() != null);
        verify(createIncomePort, times(2)).createIncome(any());
    }

    @Test
    void importDoesNotCreateAnyWhenOneRowIsInvalid() {
        givenCurrentUser();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", new BigDecimal("5000000"), List.of()),
                new IncomeImportParsedRow(3, null, "", "X", null, List.of("Fecha invalida"))
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "salario")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        var response = useCase.importIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows()).anyMatch(row -> !row.valid());
        verify(createIncomePort, never()).createIncome(any());
    }

    @Test
    void previewReturnsParsedRowDataAndDoesNotCreate() {
        givenCurrentUser();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", new BigDecimal("5000000"), List.of())
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "salario"))
                .thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        var response = useCase.previewIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().incomeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.rows().getFirst().description()).isEqualTo("Nomina");
        assertThat(response.rows().getFirst().categoryName()).isEqualTo("Salario");
        assertThat(response.rows().getFirst().categoryId()).isEqualTo(10L);
        assertThat(response.rows().getFirst().amount()).isEqualByComparingTo("5000000");
        verify(createIncomePort, never()).createIncome(any());
    }

    @Test
    void importFailsForInvalidFileExtension() {
        givenCurrentUser();

        assertThatThrownBy(() -> useCase.importIncomes(command("incomes.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateReturnsOnlyActiveIncomeCategories() {
        givenCurrentUser();
        when(categoryRepository.findActiveIncomeByAccountId(1L)).thenReturn(List.of(
                Category.restore(10L, 1L, "Salario", null, CategoryType.INCOME, CatalogStatus.ACTIVE, Instant.now(), Instant.now())
        ));
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-income-import-template.xlsx");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    private static ImportIncomeCommand command(String filename) {
        return new ImportIncomeCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }
}
