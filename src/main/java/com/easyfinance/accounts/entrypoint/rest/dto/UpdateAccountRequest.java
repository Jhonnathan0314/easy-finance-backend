package com.easyfinance.accounts.entrypoint.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank(message = "Account name is required.")
        @Size(max = 120, message = "Account name must contain at most 120 characters.")
        String name,

        @Size(max = 500, message = "Account description must contain at most 500 characters.")
        String description
) {
}
