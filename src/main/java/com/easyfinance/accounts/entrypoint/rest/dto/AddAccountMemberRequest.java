package com.easyfinance.accounts.entrypoint.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddAccountMemberRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        String email,

        @NotNull(message = "Role is required.")
        AccountRoleDto role
) {
}
