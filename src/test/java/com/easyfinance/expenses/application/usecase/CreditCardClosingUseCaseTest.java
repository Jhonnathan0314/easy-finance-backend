package com.easyfinance.expenses.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.application.query.CreditCardClosingQuery;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditCardClosingUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final CatalogValidationPort catalogValidationPort = mock(CatalogValidationPort.class);
    private final GetCategoryPort getCategoryPort = mock(GetCategoryPort.class);
    private final ExpenseRepositoryPort expenseRepository = mock(ExpenseRepositoryPort.class);
    private final AccountAuthorizationService accountAuthorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final CreditCardClosingUseCase useCase = new CreditCardClosingUseCase(currentUserProvider, accountAuthorizationService, catalogValidationPort, getCategoryPort, expenseRepository);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void nonAdminCannotPreviewClosing() {
        givenMemberAccess(AccountParticipantRole.ACCOUNT_MEMBER);

        assertThatThrownBy(() -> useCase.previewClosing(command()))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));

        verify(expenseRepository, never()).findEligibleForClosing(any());
    }

    @Test
    void previewRejectsPaymentMethodThatIsNotCreditCard() {
        givenMemberAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L))
                .thenReturn(Optional.of(new PaymentMethodValidationView(3L, 1L, PaymentMethodType.CASH, CatalogStatus.ACTIVE)));

        assertThatThrownBy(() -> useCase.previewClosing(command()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CREDIT_CARD_CLOSING_INVALID_PAYMENT_METHOD"));
    }

    @Test
    void previewGroupsEligibleExpensesByCategory() {
        givenMemberAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenCreditCardPaymentMethod();
        Expense groceries1 = expense(2L, "10000");
        Expense groceries2 = expense(2L, "5000");
        Expense transport = expense(9L, "20000");
        when(expenseRepository.findEligibleForClosing(any(CreditCardClosingQuery.class)))
                .thenReturn(List.of(groceries1, groceries2, transport));
        when(getCategoryPort.getCategory(1L, 2L)).thenReturn(categoryResponse(2L, "Groceries"));
        when(getCategoryPort.getCategory(1L, 9L)).thenReturn(categoryResponse(9L, "Transport"));

        CreditCardClosingPreviewResponse preview = useCase.previewClosing(command());

        assertThat(preview.totalCount()).isEqualTo(3);
        assertThat(preview.totalAmount()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(preview.byCategory()).hasSize(2);
        assertThat(preview.byCategory()).anySatisfy(item -> {
            assertThat(item.categoryId()).isEqualTo(2L);
            assertThat(item.categoryName()).isEqualTo("Groceries");
            assertThat(item.amount()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(item.count()).isEqualTo(2);
        });
    }

    @Test
    void confirmClosingMarksEligibleExpensesAsPaid() {
        givenMemberAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenCreditCardPaymentMethod();
        Expense pending = expense(2L, "10000");
        when(expenseRepository.findEligibleForClosing(any(CreditCardClosingQuery.class))).thenReturn(List.of(pending));
        when(expenseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardClosingResultResponse result = useCase.confirmClosing(command());

        ArgumentCaptor<List<Expense>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).paymentState()).isEqualTo(ExpensePaymentState.PAID);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void confirmClosingRejectsEmptyResult() {
        givenMemberAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenCreditCardPaymentMethod();
        when(expenseRepository.findEligibleForClosing(any(CreditCardClosingQuery.class))).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.confirmClosing(command()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CREDIT_CARD_CLOSING_EMPTY"));

        verify(expenseRepository, never()).saveAll(any());
    }

    private void givenMemberAccess(AccountParticipantRole role) {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, role, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenCreditCardPaymentMethod() {
        when(catalogValidationPort.findPaymentMethodForValidation(1L, 3L))
                .thenReturn(Optional.of(new PaymentMethodValidationView(3L, 1L, PaymentMethodType.CREDIT_CARD, CatalogStatus.ACTIVE)));
    }

    private static CreditCardClosingCommand command() {
        return new CreditCardClosingCommand(1L, 3L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    }

    private static Expense expense(Long categoryId, String amount) {
        return Expense.createSimple(1L, categoryId, 3L, 10L, "Purchase", Money.cop(new BigDecimal(amount)), LocalDate.of(2026, 1, 15), ExpensePaymentState.PENDING);
    }

    private static CategoryResponse categoryResponse(Long id, String name) {
        return new CategoryResponse(id, 1L, name, null, "EXPENSE", "ACTIVE", Instant.now(), Instant.now());
    }
}
