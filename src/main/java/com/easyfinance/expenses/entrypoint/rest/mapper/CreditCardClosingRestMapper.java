package com.easyfinance.expenses.entrypoint.rest.mapper;

import com.easyfinance.expenses.application.command.CreditCardClosingCommand;
import com.easyfinance.expenses.application.response.CreditCardClosingCategoryItem;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingCategoryItemDto;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingPreviewResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingResultResponseDto;

import java.time.LocalDate;

public final class CreditCardClosingRestMapper {

    private CreditCardClosingRestMapper() {
    }

    public static CreditCardClosingCommand toCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to) {
        return new CreditCardClosingCommand(accountId, paymentMethodId, from, to);
    }

    public static CreditCardClosingCommand toCommand(Long accountId, CreditCardClosingRequest request) {
        return new CreditCardClosingCommand(accountId, request.paymentMethodId(), request.from(), request.to());
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
                response.updatedCount()
        );
    }

    private static CreditCardClosingCategoryItemDto toDto(CreditCardClosingCategoryItem item) {
        return new CreditCardClosingCategoryItemDto(item.categoryId(), item.categoryName(), item.amount(), item.count());
    }
}
