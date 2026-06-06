package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
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
import com.easyfinance.shared.domain.ForbiddenOperationException;
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
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncomeImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final AssignedParticipantValidator assignedParticipantValidator = mock(AssignedParticipantValidator.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final ParticipantLookupPort participantLookupPort = mock(ParticipantLookupPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final IncomeImportParserPort parserPort = mock(IncomeImportParserPort.class);
    private final IncomeImportTemplateGeneratorPort templateGeneratorPort = mock(IncomeImportTemplateGeneratorPort.class);
    private final CreateIncomePort createIncomePort = mock(CreateIncomePort.class);
    private final IncomeImportUseCase useCase = new IncomeImportUseCase(
            currentUserProvider,
            accountAuthorizationService,
            assignedParticipantValidator,
            accountParticipantRepository,
            participantLookupPort,
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
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", null, new BigDecimal("5000000"), List.of()),
                new IncomeImportParsedRow(3, LocalDate.of(2026, 5, 12), "Freelance", "Servicios", "Ana Gomez <ana@example.com>", new BigDecimal("700000"), List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(null))).thenReturn(10L);
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L))).thenReturn(20L);
        when(catalogValidationPort.findCategoryForValidation(1L, "salario")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));
        when(catalogValidationPort.findCategoryForValidation(1L, "servicios")).thenReturn(Optional.of(new CategoryValidationView(11L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));
        when(createIncomePort.createIncome(any()))
                .thenReturn(new IncomeResponse(101L, 1L, 10L, 10L, "Nomina", new BigDecimal("5000000"), "COP", LocalDate.of(2026, 5, 10), "ACTIVE", Instant.now(), Instant.now()))
                .thenReturn(new IncomeResponse(102L, 1L, 11L, 10L, "Freelance", new BigDecimal("700000"), "COP", LocalDate.of(2026, 5, 12), "ACTIVE", Instant.now(), Instant.now()));

        var response = useCase.importIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.rows()).allMatch(row -> row.valid() && row.createdIncomeId() != null);
        assertThat(response.rows().getFirst().participantId()).isEqualTo(10L);
        assertThat(response.rows().get(1).participantId()).isEqualTo(20L);
        verify(createIncomePort, times(2)).createIncome(any());
    }

    @Test
    void importDoesNotCreateAnyWhenOneRowIsInvalid() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", null, new BigDecimal("5000000"), List.of()),
                new IncomeImportParsedRow(3, null, "", "X", null, null, List.of("Fecha invalida"))
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(null))).thenReturn(10L);
        when(catalogValidationPort.findCategoryForValidation(1L, "salario")).thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        var response = useCase.importIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows()).anyMatch(row -> !row.valid());
        verify(createIncomePort, never()).createIncome(any());
    }

    @Test
    void previewReturnsParsedRowDataAndDoesNotCreate() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", "Ana Gomez <ana@example.com>", new BigDecimal("5000000"), List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L))).thenReturn(20L);
        when(catalogValidationPort.findCategoryForValidation(1L, "salario"))
                .thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        var response = useCase.previewIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().incomeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.rows().getFirst().description()).isEqualTo("Nomina");
        assertThat(response.rows().getFirst().categoryName()).isEqualTo("Salario");
        assertThat(response.rows().getFirst().categoryId()).isEqualTo(10L);
        assertThat(response.rows().getFirst().participantLabel()).isEqualTo("Ana Gomez <ana@example.com>");
        assertThat(response.rows().getFirst().participantId()).isEqualTo(20L);
        assertThat(response.rows().getFirst().amount()).isEqualByComparingTo("5000000");
        verify(createIncomePort, never()).createIncome(any());
    }

    @Test
    void importPassesAssignedParticipantIdToCreateIncomeCommand() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", "ana@example.com", new BigDecimal("5000000"), List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L))).thenReturn(20L);
        when(catalogValidationPort.findCategoryForValidation(1L, "salario"))
                .thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));
        when(createIncomePort.createIncome(any()))
                .thenReturn(new IncomeResponse(101L, 1L, 10L, 20L, "Nomina", new BigDecimal("5000000"), "COP", LocalDate.of(2026, 5, 10), "ACTIVE", Instant.now(), Instant.now()));

        useCase.importIncomes(command("incomes.xlsx"));

        var captor = forClass(com.easyfinance.income.application.command.CreateIncomeCommand.class);
        verify(createIncomePort).createIncome(captor.capture());
        assertThat(captor.getValue().participantId()).isEqualTo(20L);
    }

    @Test
    void importDoesNotCreateWhenMemberAssignsAnotherParticipant() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_MEMBER);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new IncomeImportParsedRow(2, LocalDate.of(2026, 5, 10), "Nomina", "Salario", "Ana Gomez <ana@example.com>", new BigDecimal("5000000"), List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L)))
                .thenThrow(new ForbiddenOperationException("ASSIGNED_PARTICIPANT_NOT_ALLOWED", "Only account admins can assign records to another participant."));
        when(catalogValidationPort.findCategoryForValidation(1L, "salario"))
                .thenReturn(Optional.of(new CategoryValidationView(10L, 1L, CategoryType.INCOME, CatalogStatus.ACTIVE)));

        var response = useCase.importIncomes(command("incomes.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().valid()).isFalse();
        assertThat(response.rows().getFirst().errors()).contains("Participante no permitido para el usuario actual");
        verify(createIncomePort, never()).createIncome(any());
    }

    @Test
    void importFailsForInvalidFileExtension() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);

        assertThatThrownBy(() -> useCase.importIncomes(command("incomes.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateReturnsOnlyActiveIncomeCategories() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_MEMBER);
        givenParticipants();
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

    private void givenAccountAccess(AccountParticipantRole role) {
        var access = new AccountAccess(
                Account.restore(1L, "Casa", null, AccountStatus.ACTIVE, Instant.now(), Instant.now()),
                AccountParticipant.restore(1L, 1L, 10L, role, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now())
        );
        when(accountAuthorizationService.requireActiveMemberForActiveAccount(1L, 10L)).thenReturn(access);
        when(accountAuthorizationService.requireActiveMember(1L, 10L)).thenReturn(access);
    }

    private void givenParticipants() {
        when(accountParticipantRepository.findByAccountId(1L)).thenReturn(List.of(
                AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now()),
                AccountParticipant.restore(2L, 1L, 20L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now())
        ));
        when(participantLookupPort.findByParticipantIds(List.of(10L, 20L))).thenReturn(Map.of(
                10L, new ParticipantInfo(10L, 1L, "user@example.com", "Usuario Actual", true),
                20L, new ParticipantInfo(20L, 2L, "ana@example.com", "Ana Gomez", true)
        ));
    }

    private static ImportIncomeCommand command(String filename) {
        return new ImportIncomeCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }
}
