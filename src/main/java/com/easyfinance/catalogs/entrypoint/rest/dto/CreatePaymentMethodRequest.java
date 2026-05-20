package com.easyfinance.catalogs.entrypoint.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentMethodRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        PaymentMethodTypeDto type
) {
}
