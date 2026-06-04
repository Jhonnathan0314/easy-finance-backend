package com.easyfinance.imports.entrypoint.rest.mapper;

import com.easyfinance.imports.application.response.CategoryImportResponse;
import com.easyfinance.imports.entrypoint.rest.dto.CategoryImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.dto.CategoryImportRowResponseDto;

public final class CategoryImportRestMapper {

    private CategoryImportRestMapper() {
    }

    public static CategoryImportResponseDto toDto(CategoryImportResponse response) {
        return new CategoryImportResponseDto(
                response.createdCount(),
                response.rows().stream()
                        .map(row -> new CategoryImportRowResponseDto(
                                row.rowNumber(),
                                row.name(),
                                row.description(),
                                row.type(),
                                row.valid(),
                                row.createdCategoryId(),
                                row.errors()
                        ))
                        .toList()
        );
    }
}
