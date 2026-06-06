package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;
import com.easyfinance.imports.application.response.ExpenseImportRowResponse;
import com.easyfinance.imports.application.response.ImportRowErrorResponse;
import com.easyfinance.imports.entrypoint.rest.dto.ExpenseImportBatchResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.ExpenseImportRowResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.ImportRowErrorDto;

public final class ExpenseImportRestMapper {

    private ExpenseImportRestMapper() {
    }

    public static ExpenseImportBatchResponseDto toDto(ExpenseImportBatchResponse response) {
        return new ExpenseImportBatchResponseDto(
                response.batchId(),
                response.accountId(),
                response.participantId(),
                response.originalFilename(),
                response.status(),
                response.totalRows(),
                response.validRows(),
                response.invalidRows(),
                response.confirmedAt(),
                response.rows().stream().map(ExpenseImportRestMapper::toDto).toList()
        );
    }

    private static ExpenseImportRowResponseDto toDto(ExpenseImportRowResponse response) {
        return new ExpenseImportRowResponseDto(
                response.id(),
                response.rowNumber(),
                response.expenseDate(),
                response.description(),
                response.amount(),
                response.currency(),
                response.categoryName(),
                response.categoryId(),
                response.paymentMethodName(),
                response.paymentMethodId(),
                response.paymentState(),
                response.participantLabel(),
                response.participantId(),
                response.appliesDebtPayment(),
                response.debtId(),
                response.debtLabel(),
                response.debtPaymentType(),
                response.debtPaymentNotes(),
                response.valid(),
                response.errors().stream().map(ExpenseImportRestMapper::toDto).toList(),
                response.createdExpenseId(),
                response.createdDebtPaymentId()
        );
    }

    private static ImportRowErrorDto toDto(ImportRowErrorResponse response) {
        return new ImportRowErrorDto(response.column(), response.code(), response.message());
    }
}
