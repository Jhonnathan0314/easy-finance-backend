package com.easyfinance.catalogs.entrypoint.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePaymentMethodRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        PaymentMethodTypeDto type
) {
}
