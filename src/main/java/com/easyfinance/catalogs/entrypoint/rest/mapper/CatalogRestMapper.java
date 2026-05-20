package com.easyfinance.catalogs.entrypoint.rest.mapper;

import com.easyfinance.catalogs.application.command.CreateCategoryCommand;
import com.easyfinance.catalogs.application.command.CreatePaymentMethodCommand;
import com.easyfinance.catalogs.application.command.UpdateCategoryCommand;
import com.easyfinance.catalogs.application.command.UpdatePaymentMethodCommand;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.catalogs.entrypoint.rest.dto.CategoryResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.CreateCategoryRequest;
import com.easyfinance.catalogs.entrypoint.rest.dto.CreatePaymentMethodRequest;
import com.easyfinance.catalogs.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.PaymentMethodResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.UpdateCategoryRequest;
import com.easyfinance.catalogs.entrypoint.rest.dto.UpdatePaymentMethodRequest;

public final class CatalogRestMapper {

    private CatalogRestMapper() {
    }

    public static CreateCategoryCommand toCommand(Long accountId, CreateCategoryRequest request) {
        return new CreateCategoryCommand(accountId, request.name(), request.description(), CategoryType.valueOf(request.type().name()));
    }

    public static UpdateCategoryCommand toCommand(Long accountId, Long categoryId, UpdateCategoryRequest request) {
        return new UpdateCategoryCommand(
                accountId,
                categoryId,
                request.name(),
                request.description(),
                request.type() == null ? null : CategoryType.valueOf(request.type().name())
        );
    }

    public static CategoryResponseDto toDto(CategoryResponse response) {
        return new CategoryResponseDto(
                response.id(),
                response.accountId(),
                response.name(),
                response.description(),
                response.type(),
                response.status(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static CreatePaymentMethodCommand toCommand(Long accountId, CreatePaymentMethodRequest request) {
        return new CreatePaymentMethodCommand(accountId, request.name(), request.description(), PaymentMethodType.valueOf(request.type().name()));
    }

    public static UpdatePaymentMethodCommand toCommand(Long accountId, Long paymentMethodId, UpdatePaymentMethodRequest request) {
        return new UpdatePaymentMethodCommand(
                accountId,
                paymentMethodId,
                request.name(),
                request.description(),
                request.type() == null ? null : PaymentMethodType.valueOf(request.type().name())
        );
    }

    public static PaymentMethodResponseDto toDto(PaymentMethodResponse response) {
        return new PaymentMethodResponseDto(
                response.id(),
                response.accountId(),
                response.name(),
                response.description(),
                response.type(),
                response.status(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static <T, R> PageResponseDto<R> toDto(PageResponse<T> response, java.util.function.Function<T, R> mapper) {
        return new PageResponseDto<>(
                response.content().stream().map(mapper).toList(),
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
    }
}
