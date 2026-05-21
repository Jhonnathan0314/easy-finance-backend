package com.easyfinance.expenses.entrypoint.rest.mapper;

import com.easyfinance.expenses.application.command.CreateExpenseCommand;
import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.command.DuplicateExpenseCommand;
import com.easyfinance.expenses.application.command.UpdateExpenseCommand;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.entrypoint.rest.dto.CreateExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.CreateInstallmentExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.DuplicateExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.ExpenseResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.UpdateExpenseRequest;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.util.function.Function;

public final class ExpenseRestMapper {

    private ExpenseRestMapper() {
    }

    public static CreateExpenseCommand toCommand(Long accountId, CreateExpenseRequest request) {
        return new CreateExpenseCommand(
                accountId,
                request.categoryId(),
                request.paymentMethodId(),
                request.description(),
                toPositiveCop(request.amount()),
                request.expenseDate(),
                request.paymentState() == null ? null : ExpensePaymentState.valueOf(request.paymentState().name())
        );
    }

    public static CreateInstallmentExpenseCommand toCommand(Long accountId, CreateInstallmentExpenseRequest request) {
        return new CreateInstallmentExpenseCommand(
                accountId,
                request.categoryId(),
                request.paymentMethodId(),
                request.description(),
                toPositiveCop(request.totalAmount()),
                request.expenseDate(),
                request.installmentCount(),
                toPositiveCop(request.installmentAmount()),
                request.firstInstallmentDate(),
                request.debtName(),
                request.notes()
        );
    }

    public static UpdateExpenseCommand toCommand(Long accountId, Long expenseId, UpdateExpenseRequest request) {
        return new UpdateExpenseCommand(
                accountId,
                expenseId,
                request.categoryId(),
                request.paymentMethodId(),
                request.description(),
                toPositiveCop(request.amount()),
                request.expenseDate(),
                ExpensePaymentState.valueOf(request.paymentState().name())
        );
    }

    public static DuplicateExpenseCommand toCommand(Long accountId, Long expenseId, DuplicateExpenseRequest request) {
        return new DuplicateExpenseCommand(
                accountId,
                expenseId,
                request.expenseDate(),
                request.amount() == null ? null : toPositiveCop(request.amount()),
                request.description(),
                request.paymentState() == null ? null : ExpensePaymentState.valueOf(request.paymentState().name())
        );
    }

    public static ExpenseResponseDto toDto(ExpenseResponse response) {
        return new ExpenseResponseDto(
                response.id(),
                response.accountId(),
                response.categoryId(),
                response.paymentMethodId(),
                response.participantId(),
                response.description(),
                response.amount(),
                response.currency(),
                response.expenseDate(),
                response.paymentState(),
                response.status(),
                response.expenseType(),
                response.sourceType(),
                response.sourceDebtPaymentId(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static <T, R> PageResponseDto<R> toDto(PageResponse<T> response, Function<T, R> mapper) {
        return new PageResponseDto<>(
                response.content().stream().map(mapper).toList(),
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
    }

    private static Money toPositiveCop(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("EXPENSE_AMOUNT_INVALID", "Expense amount must be greater than zero in COP.");
        }
        return Money.positive(amount, CurrencyCode.COP);
    }
}
