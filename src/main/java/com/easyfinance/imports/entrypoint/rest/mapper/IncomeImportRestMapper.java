package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.IncomeImportResponse;
import com.easyfinance.imports.entrypoint.rest.dto.IncomeImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.IncomeImportRowResponseDto;

public final class IncomeImportRestMapper {

    private IncomeImportRestMapper() {
    }

    public static IncomeImportResponseDto toDto(IncomeImportResponse response) {
        return new IncomeImportResponseDto(
                response.createdCount(),
                response.rows().stream()
                        .map(row -> new IncomeImportRowResponseDto(
                                row.rowNumber(),
                                row.incomeDate(),
                                row.description(),
                                row.categoryName(),
                                row.categoryId(),
                                row.amount(),
                                row.valid(),
                                row.createdIncomeId(),
                                row.errors()
                        ))
                        .toList()
        );
    }
}
