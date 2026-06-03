package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.budgets.application.port.out.BudgetRepositoryPort;
import com.easyfinance.budgets.application.port.out.SubBudgetRepositoryPort;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.port.out.AnnualBudgetImportParserPort;
import com.easyfinance.imports.application.port.out.AnnualBudgetImportTemplateGeneratorPort;
import com.easyfinance.imports.application.template.AnnualBudgetImportTemplateData;
import com.easyfinance.imports.application.validation.AnnualBudgetImportMonthScope;
import com.easyfinance.imports.application.validation.AnnualBudgetImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final AnnualBudgetImportParserPort parserPort = mock(AnnualBudgetImportParserPort.class);
    private final AnnualBudgetImportTemplateGeneratorPort templateGeneratorPort = mock(AnnualBudgetImportTemplateGeneratorPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final BudgetRepositoryPort budgetRepository = mock(BudgetRepositoryPort.class);
    private final SubBudgetRepositoryPort subBudgetRepository = mock(SubBudgetRepositoryPort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final BudgetImportUseCase useCase = new BudgetImportUseCase(
            currentUserProvider,
            authorizationService,
            parserPort,
            templateGeneratorPort,
            categoryRepository,
            catalogValidationPort,
            budgetRepository,
            subBudgetRepository,
            5_242_880
    );

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "admin@example.com", Set.of("USER"), true)));
    }

    @Test
    void createsBudgetsAndAppliesMonthlyOverride() {
        givenAdminAccess();
        ImportAnnualBudgetCommand command = command();
        when(parserPort.parse(command)).thenReturn(List.of(
                parsedRow(2, 2026, new AnnualBudgetImportMonthScope.AllMonths(), "Presupuesto 2026", "Mercado", "Mercado Casa", "800000"),
                parsedRow(3, 2026, new AnnualBudgetImportMonthScope.SingleMonth(3), "Presupuesto 2026", "Mercado", "Mercado Casa", "950000")
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "mercado"))
                .thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        for (int month = 1; month <= 12; month++) {
            when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, month)).thenReturn(Optional.empty());
        }
        AtomicLong ids = new AtomicLong(100);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            return Budget.restore(ids.incrementAndGet(), budget.accountId(), budget.year(), budget.month(), budget.name(), budget.status(), Instant.now(), Instant.now());
        });
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.importAnnualBudget(command);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.createdBudgetsCount()).isEqualTo(12);
        assertThat(response.createdSubBudgetsCount()).isEqualTo(12);
        verify(subBudgetRepository, times(12)).save(any(SubBudget.class));
    }

    @Test
    void returnsValidationRowsWithoutCreatingWhenHasErrors() {
        givenAdminAccess();
        ImportAnnualBudgetCommand command = command();
        when(parserPort.parse(command)).thenReturn(List.of(
                parsedRow(2, 2026, new AnnualBudgetImportMonthScope.AllMonths(), "Presupuesto 2026", "Mercado", "Mercado Casa", "800000"),
                new AnnualBudgetImportParsedRow(3, 2027, new AnnualBudgetImportMonthScope.SingleMonth(3), "Otro nombre", "", "", BigDecimal.ZERO, List.of("Mes inválido"))
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "mercado"))
                .thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));

        var response = useCase.importAnnualBudget(command);

        assertThat(response.createdBudgetsCount()).isZero();
        assertThat(response.createdSubBudgetsCount()).isZero();
        verify(budgetRepository, times(0)).save(any(Budget.class));
    }

    @Test
    void failsWhenAnyBudgetExistsInYear() {
        givenAdminAccess();
        ImportAnnualBudgetCommand command = command();
        when(parserPort.parse(command)).thenReturn(List.of(
                parsedRow(2, 2026, new AnnualBudgetImportMonthScope.AllMonths(), "Presupuesto 2026", "Mercado", "Mercado Casa", "800000")
        ));
        when(catalogValidationPort.findCategoryForValidation(1L, "mercado"))
                .thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 1))
                .thenReturn(Optional.of(Budget.restore(1L, 1L, 2026, 1, "Enero", com.easyfinance.budgets.domain.model.BudgetStatus.ACTIVE, Instant.now(), Instant.now())));

        assertThatThrownBy(() -> useCase.importAnnualBudget(command))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS"));
    }

    @Test
    void templateUsesActiveExpenseCategories() {
        givenAdminAccess();
        when(categoryRepository.findActiveExpenseByAccountId(1L)).thenReturn(List.of(
                com.easyfinance.catalogs.domain.model.Category.restore(1L, 1L, "Mercado", "mercado", CategoryType.EXPENSE, CatalogStatus.ACTIVE, Instant.now(), Instant.now()),
                com.easyfinance.catalogs.domain.model.Category.restore(2L, 1L, "Servicios", "servicios", CategoryType.EXPENSE, CatalogStatus.ACTIVE, Instant.now(), Instant.now())
        ));
        when(templateGeneratorPort.generate(any(AnnualBudgetImportTemplateData.class))).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).contains("annual-budget-import-template");
        assertThat(response.content()).hasSize(3);
    }

    @Test
    void memberCannotImport() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        assertThatThrownBy(() -> useCase.importAnnualBudget(command()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private static AnnualBudgetImportParsedRow parsedRow(int rowNumber, int year, AnnualBudgetImportMonthScope monthScope, String budgetName, String category, String subBudgetName, String amount) {
        return new AnnualBudgetImportParsedRow(rowNumber, year, monthScope, budgetName, category, subBudgetName, new BigDecimal(amount), List.of());
    }

    private ImportAnnualBudgetCommand command() {
        return new ImportAnnualBudgetCommand(1L, "budget.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 50, new ByteArrayInputStream(new byte[]{1}));
    }

    private void givenAdminAccess() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
    }

    private void givenAccess(AccountParticipantRole role, AccountStatus status) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Cuenta", null, status, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, role, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }
}
