package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.DebtImportResponse;
import com.easyfinance.imports.entrypoint.rest.dto.DebtImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.DebtImportRowResponseDto;

public final class DebtImportRestMapper {

    private DebtImportRestMapper() {
    }

    public static DebtImportResponseDto toDto(DebtImportResponse response) {
        return new DebtImportResponseDto(
                response.createdCount(),
                response.rows().stream()
                        .map(row -> new DebtImportRowResponseDto(
                                row.rowNumber(),
                                row.name(),
                                row.description(),
                                row.totalAmount(),
                                row.remainingBalance(),
                                row.installmentCount(),
                                row.installmentAmount(),
                                row.startDate(),
                                row.dueDate(),
                                row.participantLabel(),
                                row.participantId(),
                                row.notes(),
                                row.valid(),
                                row.createdDebtId(),
                                row.errors()
                        ))
                        .toList()
        );
    }
}
