package com.easyfinance.imports.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseImportRowResponse(
        Long id,
        Integer rowNumber,
        LocalDate expenseDate,
        String description,
        BigDecimal amount,
        String currency,
        String categoryName,
        Long categoryId,
        String paymentMethodName,
        Long paymentMethodId,
        String paymentState,
        String participantLabel,
        Long participantId,
        boolean appliesDebtPayment,
        Long debtId,
        String debtLabel,
        String debtPaymentType,
        String debtPaymentNotes,
        boolean valid,
        List<ImportRowErrorResponse> errors,
        Long createdExpenseId,
        Long createdDebtPaymentId
) {
    public ExpenseImportRowResponse(
            Long id,
            Integer rowNumber,
            LocalDate expenseDate,
            String description,
            BigDecimal amount,
            String currency,
            String categoryName,
            Long categoryId,
            String paymentMethodName,
            Long paymentMethodId,
            String paymentState,
            boolean appliesDebtPayment,
            Long debtId,
            String debtLabel,
            String debtPaymentType,
            String debtPaymentNotes,
            boolean valid,
            List<ImportRowErrorResponse> errors,
            Long createdExpenseId,
            Long createdDebtPaymentId
    ) {
        this(id, rowNumber, expenseDate, description, amount, currency, categoryName, categoryId, paymentMethodName, paymentMethodId, paymentState, null, null, appliesDebtPayment, debtId, debtLabel, debtPaymentType, debtPaymentNotes, valid, errors, createdExpenseId, createdDebtPaymentId);
    }
}
