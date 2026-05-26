package com.easyfinance.debts.entrypoint.rest.mapper;

import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.entrypoint.rest.dto.CreateManualDebtRequest;
import com.easyfinance.debts.entrypoint.rest.dto.DebtResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.util.function.Function;

public final class DebtRestMapper {

    private DebtRestMapper() {
    }

    public static CreateManualDebtCommand toCommand(Long accountId, CreateManualDebtRequest request) {
        return new CreateManualDebtCommand(
                accountId,
                request.name(),
                request.description(),
                toPositiveCop(request.totalAmount(), "DEBT_AMOUNT_INVALID"),
                request.installmentCount(),
                request.installmentAmount() == null ? null : toPositiveCop(request.installmentAmount(), "DEBT_INSTALLMENT_AMOUNT_INVALID"),
                request.startDate(),
                request.dueDate(),
                request.notes()
        );
    }

    public static DebtResponseDto toDto(DebtResponse response) {
        return new DebtResponseDto(
                response.id(),
                response.accountId(),
                response.participantId(),
                response.originExpenseId(),
                response.sourceType(),
                response.name(),
                response.description(),
                response.totalAmount(),
                response.scheduledTotalAmount(),
                response.totalCurrency(),
                response.remainingAmount(),
                response.remainingCurrency(),
                response.installmentCount(),
                response.installmentAmount(),
                response.installmentCurrency(),
                response.startDate(),
                response.endDate(),
                response.state(),
                response.notes(),
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

    private static Money toPositiveCop(BigDecimal amount, String code) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(code, "Amount must be greater than zero in COP.");
        }
        return Money.positive(amount, CurrencyCode.COP);
    }
}
