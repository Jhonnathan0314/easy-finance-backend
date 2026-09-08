package com.easyfinance.imports.application.validation;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;

import java.util.List;

public record PaymentMethodImportParsedRow(
        Integer rowNumber,
        String name,
        String description,
        PaymentMethodType type,
        String status,
        List<String> errors
) {
    public PaymentMethodImportParsedRow(Integer rowNumber, String name, String description, PaymentMethodType type, List<String> errors) { this(rowNumber,name,description,type,null,errors); }
}
