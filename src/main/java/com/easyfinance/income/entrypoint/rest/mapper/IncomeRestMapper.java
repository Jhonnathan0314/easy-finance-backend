package com.easyfinance.income.entrypoint.rest.mapper;

import com.easyfinance.income.application.command.CreateIncomeCommand;
import com.easyfinance.income.application.command.DuplicateIncomeCommand;
import com.easyfinance.income.application.command.UpdateIncomeCommand;
import com.easyfinance.income.application.response.IncomeResponse;
import com.easyfinance.income.application.response.PageResponse;
import com.easyfinance.income.entrypoint.rest.dto.CreateIncomeRequest;
import com.easyfinance.income.entrypoint.rest.dto.DuplicateIncomeRequest;
import com.easyfinance.income.entrypoint.rest.dto.IncomeResponseDto;
import com.easyfinance.income.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.income.entrypoint.rest.dto.UpdateIncomeRequest;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.util.function.Function;

public final class IncomeRestMapper {

    private IncomeRestMapper() {
    }

    public static CreateIncomeCommand toCommand(Long accountId, CreateIncomeRequest request) {
        return new CreateIncomeCommand(
                accountId,
                request.categoryId(),
                request.description(),
                toPositiveCop(request.amount()),
                request.incomeDate()
        );
    }

    public static UpdateIncomeCommand toCommand(Long accountId, Long incomeId, UpdateIncomeRequest request) {
        return new UpdateIncomeCommand(
                accountId,
                incomeId,
                request.categoryId(),
                request.description(),
                toPositiveCop(request.amount()),
                request.incomeDate()
        );
    }

    public static DuplicateIncomeCommand toCommand(Long accountId, Long incomeId, DuplicateIncomeRequest request) {
        return new DuplicateIncomeCommand(
                accountId,
                incomeId,
                request.incomeDate(),
                request.amount() == null ? null : toPositiveCop(request.amount()),
                request.description()
        );
    }

    public static IncomeResponseDto toDto(IncomeResponse response) {
        return new IncomeResponseDto(
                response.id(),
                response.accountId(),
                response.categoryId(),
                response.participantId(),
                response.description(),
                response.amount(),
                response.currency(),
                response.incomeDate(),
                response.status(),
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
            throw new BusinessRuleViolationException("INCOME_AMOUNT_INVALID", "Income amount must be greater than zero in COP.");
        }
        return Money.positive(amount, CurrencyCode.COP);
    }
}
