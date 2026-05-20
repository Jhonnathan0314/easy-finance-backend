package com.easyfinance.catalogs.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.catalogs.application.command.CreateCategoryCommand;
import com.easyfinance.catalogs.application.command.CreatePaymentMethodCommand;
import com.easyfinance.catalogs.application.command.UpdateCategoryCommand;
import com.easyfinance.catalogs.application.command.UpdatePaymentMethodCommand;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogManagementUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final CategoryRepositoryPort categoryRepository = mock(CategoryRepositoryPort.class);
    private final PaymentMethodRepositoryPort paymentMethodRepository = mock(PaymentMethodRepositoryPort.class);
    private final AccountAuthorizationService authorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final CatalogManagementUseCase useCase = new CatalogManagementUseCase(
            currentUserProvider,
            authorizationService,
            categoryRepository,
            paymentMethodRepository
    );

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "admin@example.com", Set.of("USER"), true)));
    }

    @Test
    void createCategoryAsAdminWorks() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.save(any(Category.class))).thenReturn(category(CatalogStatus.ACTIVE));

        var response = useCase.createCategory(new CreateCategoryCommand(1L, "Food", "Meals", CategoryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Food");
        assertThat(response.type()).isEqualTo("EXPENSE");
    }

    @Test
    void createCategoryAsMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.createCategory(new CreateCategoryCommand(1L, "Food", null, CategoryType.EXPENSE)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void updateCategoryAsMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.updateCategory(new UpdateCategoryCommand(1L, 1L, "Food", null, null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void createCategoryInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.createCategory(new CreateCategoryCommand(1L, "Food", null, CategoryType.EXPENSE)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void createCategoryDuplicatedNameFails() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedName(1L, CategoryType.EXPENSE, "food")).thenReturn(true);

        assertThatThrownBy(() -> useCase.createCategory(new CreateCategoryCommand(1L, "Food", null, CategoryType.EXPENSE)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void listCategoriesAsMemberWorksAndDefaultsAreRepositoryConcern() {
        givenMemberAccess(AccountStatus.ARCHIVED);
        when(categoryRepository.findAll(any(ListCategoriesQuery.class)))
                .thenReturn(new PageResponse<>(List.of(category(CatalogStatus.ACTIVE)), 0, 20, 1, 1));

        var response = useCase.listCategories(new ListCategoriesQuery(1L, null, null, null, PageQuery.of(0, 20), null));

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void getCategoryOutsideAccountReturnsNotFound() {
        givenMemberAccess(AccountStatus.ACTIVE);
        when(categoryRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getCategory(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_NOT_FOUND"));
    }

    @Test
    void updateInactiveCategoryFails() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.findByAccountIdAndId(1L, 1L)).thenReturn(Optional.of(category(CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.updateCategory(new UpdateCategoryCommand(1L, 1L, "Food", null, null)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_INACTIVE"));
    }

    @Test
    void updateCategoryInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.updateCategory(new UpdateCategoryCommand(1L, 1L, "Food", null, null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void updateCategoryOutsideAccountReturnsNotFound() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateCategory(new UpdateCategoryCommand(1L, 99L, "Food", null, null)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_NOT_FOUND"));
    }

    @Test
    void changingCategoryTypeFails() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.findByAccountIdAndId(1L, 1L)).thenReturn(Optional.of(category(CatalogStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updateCategory(new UpdateCategoryCommand(1L, 1L, "Food", null, CategoryType.INCOME)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_TYPE_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void deactivateCategoryInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.deactivateCategory(1L, 1L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void deactivateCategoryOutsideAccountReturnsNotFound() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(categoryRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deactivateCategory(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_NOT_FOUND"));
    }

    @Test
    void createPaymentMethodAsAdminWorks() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod(CatalogStatus.ACTIVE));

        var response = useCase.createPaymentMethod(new CreatePaymentMethodCommand(1L, "Cash", null, PaymentMethodType.CASH));

        assertThat(response.name()).isEqualTo("Cash");
        assertThat(response.type()).isEqualTo("CASH");
    }

    @Test
    void createPaymentMethodInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.createPaymentMethod(new CreatePaymentMethodCommand(1L, "Cash", null, PaymentMethodType.CASH)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void updatePaymentMethodAsMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.updatePaymentMethod(new UpdatePaymentMethodCommand(1L, 1L, "Cash", null, null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void updateInactivePaymentMethodFails() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(paymentMethodRepository.findByAccountIdAndId(1L, 1L)).thenReturn(Optional.of(paymentMethod(CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.updatePaymentMethod(new UpdatePaymentMethodCommand(1L, 1L, "Cash", null, null)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_INACTIVE"));
    }

    @Test
    void updatePaymentMethodInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.updatePaymentMethod(new UpdatePaymentMethodCommand(1L, 1L, "Cash", null, null)))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void updatePaymentMethodOutsideAccountReturnsNotFound() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(paymentMethodRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updatePaymentMethod(new UpdatePaymentMethodCommand(1L, 99L, "Cash", null, null)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_NOT_FOUND"));
    }

    @Test
    void changingPaymentMethodTypeFails() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(paymentMethodRepository.findByAccountIdAndId(1L, 1L)).thenReturn(Optional.of(paymentMethod(CatalogStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.updatePaymentMethod(new UpdatePaymentMethodCommand(1L, 1L, "Cash", null, PaymentMethodType.CREDIT_CARD)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_TYPE_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void deactivatingPaymentMethodAsMemberFails() {
        givenMemberAccess(AccountStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.deactivatePaymentMethod(1L, 1L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void deactivatePaymentMethodInArchivedAccountFails() {
        givenAdminAccess(AccountStatus.ARCHIVED);

        assertThatThrownBy(() -> useCase.deactivatePaymentMethod(1L, 1L))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void deactivatePaymentMethodOutsideAccountReturnsNotFound() {
        givenAdminAccess(AccountStatus.ACTIVE);
        when(paymentMethodRepository.findByAccountIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deactivatePaymentMethod(1L, 99L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_NOT_FOUND"));
    }

    private void givenAdminAccess(AccountStatus accountStatus) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenMemberAccess(AccountStatus accountStatus) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, accountStatus, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private static Category category(CatalogStatus status) {
        return Category.restore(1L, 1L, "Food", "Meals", CategoryType.EXPENSE, status, Instant.now(), Instant.now());
    }

    private static PaymentMethod paymentMethod(CatalogStatus status) {
        return PaymentMethod.restore(1L, 1L, "Cash", null, PaymentMethodType.CASH, status, Instant.now(), Instant.now());
    }
}
