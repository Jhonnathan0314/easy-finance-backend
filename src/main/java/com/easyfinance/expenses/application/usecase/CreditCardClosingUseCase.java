package com.easyfinance.expenses.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.port.in.ConfirmCreditCardClosingPort;
import com.easyfinance.expenses.application.port.in.PreviewCreditCardClosingPort;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.application.query.CreditCardClosingQuery;
import com.easyfinance.expenses.application.response.CreditCardClosingCategoryItem;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CreditCardClosingUseCase implements PreviewCreditCardClosingPort, ConfirmCreditCardClosingPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final GetCategoryPort getCategoryPort;
    private final ExpenseRepositoryPort expenseRepository;

    public CreditCardClosingUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            GetCategoryPort getCategoryPort,
            ExpenseRepositoryPort expenseRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.getCategoryPort = getCategoryPort;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CreditCardClosingPreviewResponse previewClosing(CreditCardClosingCommand command) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser.participantId());
        validateCreditCardPaymentMethod(command.accountId(), command.paymentMethodId());
        List<Expense> eligible = expenseRepository.findEligibleForClosing(toQuery(command));
        return buildPreview(command, eligible);
    }

    @Override
    @Transactional
    public CreditCardClosingResultResponse confirmClosing(CreditCardClosingCommand command) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser.participantId());
        validateCreditCardPaymentMethod(command.accountId(), command.paymentMethodId());
        List<Expense> eligible = expenseRepository.findEligibleForClosing(toQuery(command));
        if (eligible.isEmpty()) {
            throw new BusinessRuleViolationException("CREDIT_CARD_CLOSING_EMPTY", "There are no expenses to close in the selected range.");
        }
        List<Expense> paid = eligible.stream().map(expense -> expense.changePaymentState(ExpensePaymentState.PAID)).toList();
        List<Expense> saved = expenseRepository.saveAll(paid);
        BigDecimal total = saved.stream().map(expense -> expense.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CreditCardClosingResultResponse(command.paymentMethodId(), command.from(), command.to(), total, saved.size());
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private void validateCreditCardPaymentMethod(Long accountId, Long paymentMethodId) {
        if (paymentMethodId == null) {
            throw new BusinessRuleViolationException("EXPENSE_PAYMENT_METHOD_REQUIRED", "Expense payment method is required.");
        }
        PaymentMethodValidationView paymentMethod = catalogValidationPort.findPaymentMethodForValidation(accountId, paymentMethodId)
                .orElseThrow(() -> new NotFoundException("EXPENSE_PAYMENT_METHOD_NOT_FOUND", "Expense payment method was not found."));
        if (paymentMethod.status() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("EXPENSE_PAYMENT_METHOD_INACTIVE", "Expense payment method is inactive.");
        }
        if (paymentMethod.type() != PaymentMethodType.CREDIT_CARD) {
            throw new BusinessRuleViolationException("CREDIT_CARD_CLOSING_INVALID_PAYMENT_METHOD", "Payment method must be a credit card.");
        }
    }

    private static CreditCardClosingQuery toQuery(CreditCardClosingCommand command) {
        return new CreditCardClosingQuery(command.accountId(), command.paymentMethodId(), command.from(), command.to());
    }

    private CreditCardClosingPreviewResponse buildPreview(CreditCardClosingCommand command, List<Expense> eligible) {
        Map<Long, List<Expense>> byCategory = eligible.stream().collect(Collectors.groupingBy(Expense::categoryId));
        List<CreditCardClosingCategoryItem> items = byCategory.entrySet().stream()
                .map(entry -> {
                    String categoryName = getCategoryPort.getCategory(command.accountId(), entry.getKey()).name();
                    BigDecimal amount = entry.getValue().stream().map(expense -> expense.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CreditCardClosingCategoryItem(entry.getKey(), categoryName, amount, entry.getValue().size());
                })
                .sorted(Comparator.comparing(CreditCardClosingCategoryItem::amount).reversed())
                .toList();
        BigDecimal total = eligible.stream().map(expense -> expense.amount().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ExpenseResponse> expenseResponses = eligible.stream()
                .sorted(Comparator.comparing(Expense::expenseDate))
                .map(CreditCardClosingUseCase::toResponse)
                .toList();
        return new CreditCardClosingPreviewResponse(command.paymentMethodId(), command.from(), command.to(), total, eligible.size(), items, expenseResponses);
    }

    private static ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.id(),
                expense.accountId(),
                expense.categoryId(),
                expense.paymentMethodId(),
                expense.participantId(),
                expense.description(),
                expense.amount().amount(),
                expense.amount().currency().name(),
                expense.expenseDate(),
                expense.paymentState().name(),
                expense.status().name(),
                expense.expenseType().name(),
                expense.sourceType().name(),
                expense.sourceDebtPaymentId(),
                expense.sourceDebtId(),
                expense.createdAt(),
                expense.updatedAt()
        );
    }
}
