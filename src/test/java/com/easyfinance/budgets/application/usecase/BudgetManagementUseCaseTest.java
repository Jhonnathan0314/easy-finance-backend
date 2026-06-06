package com.easyfinance.budgets.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateAnnualBudgetCommand;
import com.easyfinance.budgets.application.command.CreateAnnualSubBudgetBaseCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.command.CreateSubBudgetCommand;
import com.easyfinance.budgets.application.command.DuplicateBudgetCommand;
import com.easyfinance.budgets.application.command.UpdateSubBudgetCommand;
import com.easyfinance.budgets.application.command.UpsertBudgetCommand;
import com.easyfinance.budgets.application.port.out.BudgetImpactRepositoryPort;
import com.easyfinance.budgets.application.port.out.BudgetExpenseExecutionQueryPort;
import com.easyfinance.budgets.application.port.out.BudgetRepositoryPort;
import com.easyfinance.budgets.application.port.out.SubBudgetRepositoryPort;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetImpact;
import com.easyfinance.budgets.domain.model.BudgetImpactStatus;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.budgets.domain.model.SubBudgetSourceType;
import com.easyfinance.budgets.domain.model.SubBudgetStatus;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BudgetManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final BudgetRepositoryPort budgetRepository = mock(BudgetRepositoryPort.class);
    private final SubBudgetRepositoryPort subBudgetRepository = mock(SubBudgetRepositoryPort.class);
    private final BudgetImpactRepositoryPort impactRepository = mock(BudgetImpactRepositoryPort.class);
    private final BudgetExpenseExecutionQueryPort expenseExecutionQueryPort = mock(BudgetExpenseExecutionQueryPort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final AssignedParticipantValidator assignedParticipantValidator = new AssignedParticipantValidator(accountParticipantRepository);
    private final BudgetManagementUseCase useCase = new BudgetManagementUseCase(currentUserProvider, authorizationService, assignedParticipantValidator, catalogValidationPort, budgetRepository, subBudgetRepository, impactRepository, expenseExecutionQueryPort);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void adminCreatesBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> persistedBudget(invocation.getArgument(0), 50L));

        var response = useCase.upsertBudget(new UpsertBudgetCommand(1L, 2026, 5, "May", null));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void memberCannotCreateBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.upsertBudget(new UpsertBudgetCommand(1L, 2026, 5, "May", null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void archivedAccountBlocksBudgetWrites() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.upsertBudget(new UpsertBudgetCommand(1L, 2026, 5, "May", null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminDuplicatesBudgetWithActiveManualSubBudgetsOnly() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        Budget source = persistedBudget(Budget.create(1L, 2026, 5, "Casa"), 50L);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(source));
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 6)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> persistedBudget(invocation.getArgument(0), 60L));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(
                SubBudget.restore(70L, 1L, 50L, 7L, null, "Food", Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("45000")), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now()),
                SubBudget.restore(71L, 1L, 50L, 8L, 5L, "Debt: Laptop", Money.cop(new BigDecimal("200000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.DEBT_DERIVED, Instant.now(), Instant.now()),
                SubBudget.restore(72L, 1L, 50L, 9L, null, "Inactive", Money.cop(new BigDecimal("50000")), Money.zeroCop(), SubBudgetStatus.INACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now())
        ));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 80L));

        var response = useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null));

        assertThat(response.budget().id()).isEqualTo(60L);
        assertThat(response.budget().name()).isEqualTo("Casa");
        assertThat(response.budget().status()).isEqualTo("ACTIVE");
        assertThat(response.subBudgets()).singleElement().satisfies(subBudget -> {
            assertThat(subBudget.budgetId()).isEqualTo(60L);
            assertThat(subBudget.categoryId()).isEqualTo(7L);
            assertThat(subBudget.name()).isEqualTo("Food");
            assertThat(subBudget.plannedAmount()).isEqualByComparingTo("100000.00");
            assertThat(subBudget.spentAmount()).isEqualByComparingTo("0.00");
            assertThat(subBudget.status()).isEqualTo("ACTIVE");
            assertThat(subBudget.sourceType()).isEqualTo("MANUAL");
        });
        assertThat(response.impacts()).isEmpty();
        verify(subBudgetRepository).save(org.mockito.ArgumentMatchers.argThat(subBudget ->
                subBudget.budgetId().equals(60L)
                        && subBudget.sourceType() == SubBudgetSourceType.MANUAL
                        && subBudget.status() == SubBudgetStatus.ACTIVE
                        && subBudget.spentAmount().isZero()));
        verifyNoInteractions(impactRepository);
    }

    @Test
    void duplicateBudgetUsesProvidedName() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        Budget source = persistedBudget(Budget.create(1L, 2026, 5, "Casa"), 50L);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(source));
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 6)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> persistedBudget(invocation.getArgument(0), 60L));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of());

        var response = useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, "Casa Junio"));

        assertThat(response.budget().name()).isEqualTo("Casa Junio");
    }

    @Test
    void duplicateBudgetFailsWhenTargetAlreadyExists() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L)));
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 6)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 6, "June"), 60L)));

        assertThatThrownBy(() -> useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("BUDGET_TARGET_ALREADY_EXISTS"));
    }

    @Test
    void duplicateBudgetFailsWhenSourceDoesNotExist() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("BUDGET_NOT_FOUND"));
    }

    @Test
    void memberCannotDuplicateBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void archivedAccountBlocksBudgetDuplicate() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminCreatesAnnualBudgetWithTwelveMonthsAndBaseSubBudgets() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        for (int month = 1; month <= 12; month++) {
            when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, month)).thenReturn(Optional.empty());
        }
        when(catalogValidationPort.findCategoryForValidation(1L, 7L)).thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> persistedBudget(invocation.getArgument(0), 100L + invocation.<Budget>getArgument(0).month()));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 200L + invocation.<SubBudget>getArgument(0).budgetId()));

        var response = useCase.createAnnualBudget(new CreateAnnualBudgetCommand(
                1L,
                2026,
                "Presupuesto 2026",
                null,
                List.of(new CreateAnnualSubBudgetBaseCommand("Mercado", 7L, Money.cop(new BigDecimal("800000"))))
        ));

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.createdBudgets()).hasSize(12);
        assertThat(response.createdBudgets().getFirst().month()).isEqualTo(1);
        assertThat(response.createdBudgets().getLast().month()).isEqualTo(12);
        verify(subBudgetRepository, times(12)).save(org.mockito.ArgumentMatchers.argThat(subBudget ->
                subBudget.sourceType() == SubBudgetSourceType.MANUAL
                        && subBudget.status() == SubBudgetStatus.ACTIVE
                        && subBudget.name().equals("Mercado")));
    }

    @Test
    void annualBudgetFailsWhenAnyMonthAlreadyExists() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 1)).thenReturn(Optional.empty());
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 2)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 2, "Feb"), 20L)));

        assertThatThrownBy(() -> useCase.createAnnualBudget(new CreateAnnualBudgetCommand(
                1L, 2026, "Presupuesto 2026", BudgetStatus.ACTIVE, List.of()
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.code()).isEqualTo("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS"));

        verify(budgetRepository, times(0)).save(any(Budget.class));
    }

    @Test
    void annualBudgetValidatesInactiveCategory() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        for (int month = 1; month <= 12; month++) {
            when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, month)).thenReturn(Optional.empty());
        }
        when(catalogValidationPort.findCategoryForValidation(1L, 7L)).thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.createAnnualBudget(new CreateAnnualBudgetCommand(
                1L,
                2026,
                "Presupuesto 2026",
                null,
                List.of(new CreateAnnualSubBudgetBaseCommand("Mercado", 7L, Money.cop(new BigDecimal("800000"))))
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.code()).isEqualTo("CATEGORY_INACTIVE"));
    }

    @Test
    void annualBudgetFailsWhenCategoryDoesNotBelongToAccount() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        for (int month = 1; month <= 12; month++) {
            when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, month)).thenReturn(Optional.empty());
        }
        when(catalogValidationPort.findCategoryForValidation(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createAnnualBudget(new CreateAnnualBudgetCommand(
                1L,
                2026,
                "Presupuesto 2026",
                null,
                List.of(new CreateAnnualSubBudgetBaseCommand("Mercado", 99L, Money.cop(new BigDecimal("800000"))))
        ))).isInstanceOfSatisfying(NotFoundException.class, ex ->
                assertThat(ex.code()).isEqualTo("CATEGORY_NOT_FOUND"));
    }

    @Test
    void annualBudgetFailsWhenPlannedAmountIsNotPositive() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        for (int month = 1; month <= 12; month++) {
            when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, month)).thenReturn(Optional.empty());
        }

        assertThatThrownBy(() -> useCase.createAnnualBudget(new CreateAnnualBudgetCommand(
                1L,
                2026,
                "Presupuesto 2026",
                null,
                List.of(new CreateAnnualSubBudgetBaseCommand("Mercado", null, Money.cop(BigDecimal.ZERO)))
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                assertThat(ex.code()).isEqualTo("PLANNED_AMOUNT_INVALID"));
    }

    @Test
    void duplicateBudgetDoesNotReadSourceFromAnotherAccount() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.duplicateBudget(new DuplicateBudgetCommand(1L, 2026, 5, 2026, 6, null)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("BUDGET_NOT_FOUND"));
        verify(budgetRepository).findByAccountIdAndYearAndMonth(1L, 2026, 5);
    }

    @Test
    void adminCreatesManualSubBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndId(1L, 50L)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L)));
        when(catalogValidationPort.findCategoryForValidation(1L, 7L)).thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 70L));

        var response = useCase.createSubBudget(new CreateSubBudgetCommand(1L, 50L, 7L, null, "Food", Money.cop(new BigDecimal("100000"))));

        assertThat(response.sourceType()).isEqualTo("MANUAL");
        assertThat(response.spentAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void adminCreatesManualSubBudgetAssignedToParticipant() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        givenAssignedParticipant(20L, AccountParticipantStatus.ACTIVE);
        when(budgetRepository.findByAccountIdAndId(1L, 50L)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L)));
        when(catalogValidationPort.findCategoryForValidation(1L, 7L)).thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 70L));

        var response = useCase.createSubBudget(new CreateSubBudgetCommand(1L, 50L, 7L, 20L, "Food", Money.cop(new BigDecimal("100000"))));

        assertThat(response.participantId()).isEqualTo(20L);
        verify(subBudgetRepository).save(org.mockito.ArgumentMatchers.argThat(subBudget -> subBudget.participantId().equals(20L)));
    }

    @Test
    void adminUpdatesManualSubBudgetParticipantAssignment() {
        givenAccess(AccountParticipantRole.ACCOUNT_ADMIN, AccountStatus.ACTIVE);
        givenAssignedParticipant(20L, AccountParticipantStatus.ACTIVE);
        SubBudget existing = SubBudget.restore(70L, 1L, 50L, 7L, null, null, "Food", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        when(budgetRepository.findByAccountIdAndId(1L, 50L)).thenReturn(Optional.of(persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L)));
        when(catalogValidationPort.findCategoryForValidation(1L, 7L)).thenReturn(Optional.of(new CategoryValidationView(7L, 1L, CategoryType.EXPENSE, CatalogStatus.ACTIVE)));
        when(subBudgetRepository.findByAccountIdAndBudgetIdAndId(1L, 50L, 70L)).thenReturn(Optional.of(existing));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 70L));

        var response = useCase.updateSubBudget(new UpdateSubBudgetCommand(1L, 50L, 70L, 7L, 20L, "Food", Money.cop(new BigDecimal("120000"))));

        assertThat(response.participantId()).isEqualTo(20L);
        assertThat(response.plannedAmount()).isEqualByComparingTo("120000.00");
    }

    @Test
    void budgetDetailUsesManualExpenseExecutionForManualSubBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        Budget budget = persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L);
        SubBudget food = SubBudget.restore(70L, 1L, 50L, 7L, null, "Food", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(budget));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(food));
        when(impactRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of());
        when(expenseExecutionQueryPort.sumManualExecutionByCategory(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), List.of(7L)))
                .thenReturn(Map.of(7L, new BigDecimal("45000.00")));

        var response = useCase.getBudget(1L, 2026, 5);

        assertThat(response.subBudgets()).singleElement().satisfies(subBudget ->
                assertThat(subBudget.spentAmount()).isEqualByComparingTo("45000.00"));
    }

    @Test
    void budgetDetailUsesParticipantExecutionForAssignedManualSubBudget() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        Budget budget = persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L);
        SubBudget food = SubBudget.restore(70L, 1L, 50L, 7L, 20L, null, "Food", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        BudgetExpenseExecutionQueryPort.CategoryParticipantKey key = new BudgetExpenseExecutionQueryPort.CategoryParticipantKey(7L, 20L);
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(budget));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(food));
        when(impactRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of());
        when(expenseExecutionQueryPort.sumManualExecutionByCategoryAndParticipant(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), List.of(key)))
                .thenReturn(Map.of(key, new BigDecimal("25000.00")));

        var response = useCase.getBudget(1L, 2026, 5);

        assertThat(response.subBudgets()).singleElement().satisfies(subBudget -> {
            assertThat(subBudget.participantId()).isEqualTo(20L);
            assertThat(subBudget.spentAmount()).isEqualByComparingTo("25000.00");
        });
    }

    @Test
    void budgetDetailDoesNotAssignManualExecutionToSubBudgetWithoutCategory() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        Budget budget = persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L);
        SubBudget uncategorized = SubBudget.restore(70L, 1L, 50L, null, null, "General", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(budget));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(uncategorized));
        when(impactRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of());

        var response = useCase.getBudget(1L, 2026, 5);

        assertThat(response.subBudgets()).singleElement().satisfies(subBudget ->
                assertThat(subBudget.spentAmount()).isEqualByComparingTo("0.00"));
        verifyNoInteractions(expenseExecutionQueryPort);
    }

    @Test
    void budgetDetailKeepsDebtDerivedExecutionFromImpacts() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        Budget budget = persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L);
        SubBudget debt = SubBudget.restore(71L, 1L, 50L, 7L, 5L, "Debt", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.DEBT_DERIVED, Instant.now(), Instant.now());
        BudgetImpact impact = BudgetImpact.restore(80L, 1L, 50L, 71L, 5L, 9L, 2026, 5, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("60000")), BudgetImpactStatus.ACTIVE, com.easyfinance.budgets.domain.model.BudgetImpactSourceType.DEBT_INSTALLMENT, Instant.now(), Instant.now());
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(budget));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(debt));
        when(impactRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(impact));

        var response = useCase.getBudget(1L, 2026, 5);

        assertThat(response.subBudgets()).singleElement().satisfies(subBudget ->
                assertThat(subBudget.spentAmount()).isEqualByComparingTo("60000.00"));
    }

    @Test
    void budgetDetailDistributesManualExecutionForDuplicatedCategoryWithoutDuplicatingTotal() {
        givenAccess(AccountParticipantRole.ACCOUNT_MEMBER, AccountStatus.ACTIVE);
        Budget budget = persistedBudget(Budget.create(1L, 2026, 5, "May"), 50L);
        SubBudget first = SubBudget.restore(70L, 1L, 50L, 7L, null, "Food", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        SubBudget second = SubBudget.restore(71L, 1L, 50L, 7L, null, "Market", Money.cop(new BigDecimal("300000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.MANUAL, Instant.now(), Instant.now());
        when(budgetRepository.findByAccountIdAndYearAndMonth(1L, 2026, 5)).thenReturn(Optional.of(budget));
        when(subBudgetRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of(first, second));
        when(impactRepository.findByAccountIdAndBudgetId(1L, 50L)).thenReturn(List.of());
        when(expenseExecutionQueryPort.sumManualExecutionByCategory(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), List.of(7L)))
                .thenReturn(Map.of(7L, new BigDecimal("80000.00")));

        var response = useCase.getBudget(1L, 2026, 5);

        assertThat(response.subBudgets()).extracting("spentAmount")
                .containsExactly(new BigDecimal("20000.00"), new BigDecimal("60000.00"));
        assertThat(response.subBudgets().stream().map(subBudget -> subBudget.spentAmount()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("80000.00");
    }

    @Test
    void createsMonthlyImpactsForInstallmentDebt() {
        when(budgetRepository.getOrCreateMonthlyBudget(any(), any(), any(), any())).thenAnswer(invocation ->
                persistedBudget(Budget.create(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3)), 100L));
        when(subBudgetRepository.findDebtDerivedByAccountIdAndBudgetIdAndDebtId(any(), any(), any())).thenReturn(Optional.empty());
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 200L));
        when(impactRepository.findByAccountIdAndDebtIdAndPeriod(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(impactRepository.save(any(BudgetImpact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(1L, 5L, 9L, 7L, 10L, "Laptop", Money.cop(new BigDecimal("300000")), 3, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 1, 31)));

        verify(impactRepository).findByAccountIdAndDebtIdAndPeriod(1L, 5L, 2026, 1);
        verify(impactRepository).findByAccountIdAndDebtIdAndPeriod(1L, 5L, 2026, 2);
        verify(impactRepository).findByAccountIdAndDebtIdAndPeriod(1L, 5L, 2026, 3);
        verify(subBudgetRepository, times(3)).save(org.mockito.ArgumentMatchers.argThat(subBudget ->
                subBudget.sourceType() == SubBudgetSourceType.DEBT_DERIVED && subBudget.participantId().equals(10L)));
    }

    @Test
    void createsMonthlyImpactsThatTotalFinancedDebtAmount() {
        when(budgetRepository.getOrCreateMonthlyBudget(any(), any(), any(), any())).thenAnswer(invocation ->
                persistedBudget(Budget.create(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3)), 100L));
        when(subBudgetRepository.findDebtDerivedByAccountIdAndBudgetIdAndDebtId(any(), any(), any())).thenReturn(Optional.empty());
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), 200L));
        when(impactRepository.findByAccountIdAndDebtIdAndPeriod(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(impactRepository.save(any(BudgetImpact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(1L, 5L, 9L, 7L, 10L, "Advance", Money.cop(new BigDecimal("1200000")), 12, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 1, 1)));

        ArgumentCaptor<BudgetImpact> impactCaptor = ArgumentCaptor.forClass(BudgetImpact.class);
        verify(impactRepository, times(12)).save(impactCaptor.capture());
        BigDecimal impactTotal = impactCaptor.getAllValues().stream()
                .map(impact -> impact.expectedAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(impactTotal).isEqualByComparingTo("1200000.00");
    }

    @Test
    void sameDebtNameCreatesDistinctDerivedSubBudgetsByDebtId() {
        when(budgetRepository.getOrCreateMonthlyBudget(any(), any(), any(), any()))
                .thenReturn(persistedBudget(Budget.create(1L, 2026, 5, "May"), 100L));
        when(subBudgetRepository.findDebtDerivedByAccountIdAndBudgetIdAndDebtId(any(), any(), any())).thenReturn(Optional.empty());
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> persistedSubBudget(invocation.getArgument(0), invocation.<SubBudget>getArgument(0).debtId() + 1000));
        when(impactRepository.findByAccountIdAndDebtIdAndPeriod(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(impactRepository.save(any(BudgetImpact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(1L, 5L, 9L, 7L, 10L, "Laptop", Money.cop(new BigDecimal("100000")), 1, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 5, 1)));
        useCase.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(1L, 6L, 10L, 7L, 10L, "Laptop", Money.cop(new BigDecimal("100000")), 1, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 5, 1)));

        verify(subBudgetRepository).findDebtDerivedByAccountIdAndBudgetIdAndDebtId(1L, 100L, 5L);
        verify(subBudgetRepository).findDebtDerivedByAccountIdAndBudgetIdAndDebtId(1L, 100L, 6L);
        verify(subBudgetRepository, times(2)).save(org.mockito.ArgumentMatchers.argThat(subBudget ->
                subBudget.sourceType() == com.easyfinance.budgets.domain.model.SubBudgetSourceType.DEBT_DERIVED
                        && (subBudget.debtId().equals(5L) || subBudget.debtId().equals(6L))));
        verify(impactRepository).save(org.mockito.ArgumentMatchers.argThat(impact -> impact.debtId().equals(5L) && impact.subBudgetId().equals(1005L)));
        verify(impactRepository).save(org.mockito.ArgumentMatchers.argThat(impact -> impact.debtId().equals(6L) && impact.subBudgetId().equals(1006L)));
    }

    @Test
    void installmentDebtTotalMismatchFails() {
        assertThatThrownBy(() -> useCase.createImpactsForInstallmentDebt(new CreateDebtBudgetImpactsCommand(1L, 5L, 9L, 7L, 10L, "Laptop", Money.cop(new BigDecimal("300001")), 3, Money.cop(new BigDecimal("100000")), LocalDate.of(2026, 1, 1))))
                .hasMessage("Installment amount multiplied by installment count must match financed debt total amount.");
    }

    @Test
    void paymentIsDistributedChronologically() {
        BudgetImpact january = impact(1L, 2026, 1, new BigDecimal("100000"), BigDecimal.ZERO);
        BudgetImpact february = impact(2L, 2026, 2, new BigDecimal("100000"), BigDecimal.ZERO);
        when(impactRepository.findActiveByAccountIdAndDebtIdOrderByPeriod(1L, 5L)).thenReturn(List.of(january, february));
        when(impactRepository.save(any(BudgetImpact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.applyDebtPaymentToImpacts(new ApplyDebtPaymentImpactCommand(1L, 5L, Money.cop(new BigDecimal("150000"))));

        verify(impactRepository).save(org.mockito.ArgumentMatchers.argThat(impact -> impact.id().equals(1L) && impact.status() == BudgetImpactStatus.PAID));
        verify(impactRepository).save(org.mockito.ArgumentMatchers.argThat(impact -> impact.id().equals(2L) && impact.paidAmount().amount().compareTo(new BigDecimal("50000.00")) == 0));
    }

    @Test
    void cancelDebtBudgetArtifactsCancelsAllNonCancelledImpactsAndDeactivatesDerivedSubBudgets() {
        BudgetImpact activeImpact = BudgetImpact.restore(1L, 1L, 10L, 20L, 5L, 9L, 2026, 5, Money.cop(new BigDecimal("100000")), Money.zeroCop(), BudgetImpactStatus.ACTIVE, com.easyfinance.budgets.domain.model.BudgetImpactSourceType.DEBT_INSTALLMENT, Instant.now(), Instant.now());
        BudgetImpact paidImpact = BudgetImpact.restore(2L, 1L, 11L, 21L, 5L, 9L, 2026, 6, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), BudgetImpactStatus.PAID, com.easyfinance.budgets.domain.model.BudgetImpactSourceType.DEBT_INSTALLMENT, Instant.now(), Instant.now());
        SubBudget derivedOne = SubBudget.restore(100L, 1L, 10L, 7L, 5L, "Debt: Laptop", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.DEBT_DERIVED, Instant.now(), Instant.now());
        SubBudget derivedTwo = SubBudget.restore(101L, 1L, 11L, 7L, 5L, "Debt: Laptop", Money.cop(new BigDecimal("100000")), Money.zeroCop(), SubBudgetStatus.ACTIVE, SubBudgetSourceType.DEBT_DERIVED, Instant.now(), Instant.now());
        when(impactRepository.findNonCancelledByAccountIdAndDebtIdOrderByPeriod(1L, 5L)).thenReturn(List.of(activeImpact, paidImpact));
        when(subBudgetRepository.findDebtDerivedActiveByAccountIdAndDebtId(1L, 5L)).thenReturn(List.of(derivedOne, derivedTwo));
        when(impactRepository.save(any(BudgetImpact.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subBudgetRepository.save(any(SubBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelActiveImpactsForDebt(1L, 5L);

        verify(impactRepository, times(2)).save(org.mockito.ArgumentMatchers.argThat(impact -> impact.status() == BudgetImpactStatus.CANCELLED));
        verify(subBudgetRepository, times(2)).save(org.mockito.ArgumentMatchers.argThat(subBudget ->
                subBudget.sourceType() == SubBudgetSourceType.DEBT_DERIVED && subBudget.status() == SubBudgetStatus.INACTIVE));
    }

    @Test
    void cancelDebtBudgetArtifactsDoesNotAffectManualSubBudgetsOrOtherDebt() {
        when(impactRepository.findNonCancelledByAccountIdAndDebtIdOrderByPeriod(1L, 5L)).thenReturn(List.of());
        when(subBudgetRepository.findDebtDerivedActiveByAccountIdAndDebtId(1L, 5L)).thenReturn(List.of());

        useCase.cancelActiveImpactsForDebt(1L, 5L);

        verify(impactRepository, times(0)).save(any(BudgetImpact.class));
        verify(subBudgetRepository, times(0)).save(any(SubBudget.class));
    }

    private void givenAccess(AccountParticipantRole role, AccountStatus accountStatus) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Account", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, role, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenAssignedParticipant(Long participantId, AccountParticipantStatus status) {
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, participantId))
                .thenReturn(Optional.of(AccountParticipant.restore(participantId, 1L, participantId, AccountParticipantRole.ACCOUNT_MEMBER, status, Instant.now(), null, null)));
    }

    private static Budget persistedBudget(Budget budget, Long id) {
        return Budget.restore(id, budget.accountId(), budget.year(), budget.month(), budget.name(), budget.status(), Instant.now(), Instant.now());
    }

    private static SubBudget persistedSubBudget(SubBudget subBudget, Long id) {
        return SubBudget.restore(id, subBudget.accountId(), subBudget.budgetId(), subBudget.categoryId(), subBudget.participantId(), subBudget.debtId(), subBudget.name(), subBudget.plannedAmount(), subBudget.spentAmount(), subBudget.status(), subBudget.sourceType(), Instant.now(), Instant.now());
    }

    private static BudgetImpact impact(Long id, Integer year, Integer month, BigDecimal expected, BigDecimal paid) {
        return BudgetImpact.restore(id, 1L, 10L, 20L, 5L, 9L, year, month, Money.cop(expected), Money.cop(paid), BudgetImpactStatus.ACTIVE, com.easyfinance.budgets.domain.model.BudgetImpactSourceType.DEBT_INSTALLMENT, Instant.now(), Instant.now());
    }
}
