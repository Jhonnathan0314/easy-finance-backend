package com.easyfinance.expenses.entrypoint.rest.mapper;

import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.response.CreditCardClosingCategoryItem;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.entrypoint.rest.dto.ExpensePaymentStateDto;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingCategoryItemDto;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingPreviewResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingResultResponseDto;

import java.time.LocalDate;
import java.util.List;

public final class CreditCardClosingRestMapper {

    private CreditCardClosingRestMapper() {
    }

    public static CreditCardClosingCommand toCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to, List<Long> categoryIds, ExpensePaymentStateDto targetState) {
        return new CreditCardClosingCommand(accountId, paymentMethodId, from, to, categoryIds, toDomain(targetState));
    }

    public static CreditCardClosingCommand toCommand(Long accountId, CreditCardClosingRequest request) {
        return new CreditCardClosingCommand(accountId, request.paymentMethodId(), request.from(), request.to(), request.categoryIds(), toDomain(request.targetState()));
    }

    private static ExpensePaymentState toDomain(ExpensePaymentStateDto state) {
        return state == null ? null : ExpensePaymentState.valueOf(state.name());
    }

    public static CreditCardClosingPreviewResponseDto toDto(CreditCardClosingPreviewResponse response) {
        return new CreditCardClosingPreviewResponseDto(
                response.paymentMethodId(),
                response.from(),
                response.to(),
                response.totalAmount(),
                response.totalCount(),
                response.byCategory().stream().map(CreditCardClosingRestMapper::toDto).toList(),
                response.expenses().stream().map(ExpenseRestMapper::toDto).toList()
        );
    }

    public static CreditCardClosingResultResponseDto toDto(CreditCardClosingResultResponse response) {
        return new CreditCardClosingResultResponseDto(
                response.paymentMethodId(),
                response.from(),
                response.to(),
                response.totalAmount(),
                response.updatedCount(),
                response.targetState().name()
        );
    }

    private static CreditCardClosingCategoryItemDto toDto(CreditCardClosingCategoryItem item) {
        return new CreditCardClosingCategoryItemDto(item.categoryId(), item.categoryName(), item.amount(), item.count());
    }
}
