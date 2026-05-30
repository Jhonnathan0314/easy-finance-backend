package com.easyfinance.budgets.entrypoint.rest.mapper;

import com.easyfinance.budgets.application.command.CreateSubBudgetCommand;
import com.easyfinance.budgets.application.command.CreateAnnualBudgetCommand;
import com.easyfinance.budgets.application.command.CreateAnnualSubBudgetBaseCommand;
import com.easyfinance.budgets.application.command.DuplicateBudgetCommand;
import com.easyfinance.budgets.application.command.UpdateSubBudgetCommand;
import com.easyfinance.budgets.application.command.UpsertBudgetCommand;
import com.easyfinance.budgets.application.response.BudgetDetailResponse;
import com.easyfinance.budgets.application.response.BudgetImpactResponse;
import com.easyfinance.budgets.application.response.BudgetResponse;
import com.easyfinance.budgets.application.response.AnnualBudgetResponse;
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.budgets.application.response.SubBudgetResponse;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetDetailResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetImpactResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.AnnualBudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.CreateAnnualBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.CreateAnnualSubBudgetBaseRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.CreateSubBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.DuplicateBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.UpdateSubBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.UpsertBudgetRequest;
import com.easyfinance.shared.domain.Money;

import java.util.function.Function;

public final class BudgetRestMapper {

    private BudgetRestMapper() {
    }

    public static UpsertBudgetCommand toCommand(Long accountId, Integer year, Integer month, UpsertBudgetRequest request) {
        return new UpsertBudgetCommand(
                accountId,
                year,
                month,
                request == null ? null : request.name(),
                request == null || request.status() == null ? null : BudgetStatus.valueOf(request.status().name())
        );
    }

    public static CreateAnnualBudgetCommand toCommand(Long accountId, CreateAnnualBudgetRequest request) {
        return new CreateAnnualBudgetCommand(
                accountId,
                request.year(),
                request.name(),
                request.status() == null ? null : BudgetStatus.valueOf(request.status().name()),
                request.subBudgets() == null ? null : request.subBudgets().stream().map(BudgetRestMapper::toCommand).toList()
        );
    }

    private static CreateAnnualSubBudgetBaseCommand toCommand(CreateAnnualSubBudgetBaseRequest request) {
        return new CreateAnnualSubBudgetBaseCommand(
                request.name(),
                request.categoryId(),
                Money.cop(request.plannedAmount())
        );
    }

    public static DuplicateBudgetCommand toCommand(Long accountId, Integer sourceYear, Integer sourceMonth, DuplicateBudgetRequest request) {
        return new DuplicateBudgetCommand(
                accountId,
                sourceYear,
                sourceMonth,
                request.targetYear(),
                request.targetMonth(),
                request.name()
        );
    }

    public static CreateSubBudgetCommand toCommand(Long accountId, Long budgetId, CreateSubBudgetRequest request) {
        return new CreateSubBudgetCommand(accountId, budgetId, request.categoryId(), request.name(), Money.cop(request.plannedAmount()));
    }

    public static UpdateSubBudgetCommand toCommand(Long accountId, Long budgetId, Long subBudgetId, UpdateSubBudgetRequest request) {
        return new UpdateSubBudgetCommand(accountId, budgetId, subBudgetId, request.categoryId(), request.name(), Money.cop(request.plannedAmount()));
    }

    public static BudgetResponseDto toDto(BudgetResponse response) {
        return new BudgetResponseDto(response.id(), response.accountId(), response.year(), response.month(), response.name(), response.status(), response.createdAt(), response.updatedAt());
    }

    public static AnnualBudgetResponseDto toDto(AnnualBudgetResponse response) {
        return new AnnualBudgetResponseDto(
                response.accountId(),
                response.year(),
                response.createdBudgets().stream().map(BudgetRestMapper::toDto).toList()
        );
    }

    public static BudgetDetailResponseDto toDto(BudgetDetailResponse response) {
        return new BudgetDetailResponseDto(
                toDto(response.budget()),
                response.subBudgets().stream().map(BudgetRestMapper::toDto).toList(),
                response.impacts().stream().map(BudgetRestMapper::toDto).toList()
        );
    }

    public static SubBudgetResponseDto toDto(SubBudgetResponse response) {
        return new SubBudgetResponseDto(
                response.id(),
                response.accountId(),
                response.budgetId(),
                response.categoryId(),
                response.debtId(),
                response.name(),
                response.plannedAmount(),
                response.plannedCurrency(),
                response.spentAmount(),
                response.spentCurrency(),
                response.status(),
                response.sourceType(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static BudgetImpactResponseDto toDto(BudgetImpactResponse response) {
        return new BudgetImpactResponseDto(
                response.id(),
                response.accountId(),
                response.budgetId(),
                response.subBudgetId(),
                response.debtId(),
                response.expenseId(),
                response.periodYear(),
                response.periodMonth(),
                response.expectedAmount(),
                response.expectedCurrency(),
                response.paidAmount(),
                response.paidCurrency(),
                response.status(),
                response.sourceType(),
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
}
