package com.easyfinance.imports.application.response;

import java.util.List;

public record CategoryImportRowResponse(
        Integer rowNumber,
        boolean valid,
        Long createdCategoryId,
        List<String> errors
) {
}

