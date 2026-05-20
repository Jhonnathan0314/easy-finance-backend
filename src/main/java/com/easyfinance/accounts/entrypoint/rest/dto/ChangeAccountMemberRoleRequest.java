package com.easyfinance.accounts.entrypoint.rest.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeAccountMemberRoleRequest(
        @NotNull(message = "Role is required.")
        AccountRoleDto role
) {
}
