package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.AnnualBudgetImportResponse;
import com.easyfinance.imports.entrypoint.rest.dto.AnnualBudgetImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.AnnualBudgetImportRowResponseDto;

public final class BudgetImportRestMapper {

    private BudgetImportRestMapper() {
    }

    public static AnnualBudgetImportResponseDto toDto(AnnualBudgetImportResponse response) {
        return new AnnualBudgetImportResponseDto(
                response.year(),
                response.createdBudgetsCount(),
                response.createdSubBudgetsCount(),
                response.rows().stream()
                        .map(row -> new AnnualBudgetImportRowResponseDto(
                                row.rowNumber(),
                                row.year(),
                                row.month(),
                                row.budgetName(),
                                row.categoryName(),
                                row.categoryId(),
                                row.subBudgetName(),
                                row.plannedAmount(),
                                row.participantLabel(),
                                row.participantId(),
                                row.valid(),
                                row.appliedMonths(),
                                row.errors()
                        ))
                        .toList()
        );
    }
}
