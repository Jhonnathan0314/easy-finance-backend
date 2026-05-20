package com.easyfinance.debts.entrypoint.rest.mapper;

import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.entrypoint.rest.dto.DebtPaymentResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.RegisterDebtPaymentRequest;
import com.easyfinance.debts.entrypoint.rest.dto.RegisterDebtPaymentResponseDto;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

import java.math.BigDecimal;
import java.util.function.Function;

public final class DebtPaymentRestMapper {

    private DebtPaymentRestMapper() {
    }

    public static RegisterDebtPaymentCommand toCommand(Long accountId, Long debtId, RegisterDebtPaymentRequest request) {
        return new RegisterDebtPaymentCommand(
                accountId,
                debtId,
                DebtPaymentType.valueOf(request.paymentType().name()),
                toPositiveCop(request.amount()),
                request.paymentDate(),
                request.notes()
        );
    }

    public static RegisterDebtPaymentResponseDto toDto(RegisterDebtPaymentResponse response) {
        return new RegisterDebtPaymentResponseDto(
                toDto(response.payment()),
                DebtRestMapper.toDto(response.debt())
        );
    }

    public static DebtPaymentResponseDto toDto(DebtPaymentResponse response) {
        return new DebtPaymentResponseDto(
                response.id(),
                response.accountId(),
                response.debtId(),
                response.participantId(),
                response.paymentType(),
                response.amount(),
                response.currency(),
                response.paymentDate(),
                response.notes(),
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
            throw new BusinessRuleViolationException("DEBT_PAYMENT_AMOUNT_INVALID", "Debt payment amount must be greater than zero in COP.");
        }
        return Money.positive(amount, CurrencyCode.COP);
    }
}
