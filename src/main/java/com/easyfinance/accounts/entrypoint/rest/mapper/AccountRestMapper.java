package com.easyfinance.accounts.entrypoint.rest.mapper;

import com.easyfinance.accounts.application.command.AddAccountMemberCommand;
import com.easyfinance.accounts.application.command.ChangeAccountMemberRoleCommand;
import com.easyfinance.accounts.application.command.CreateAccountCommand;
import com.easyfinance.accounts.application.command.UpdateAccountCommand;
import com.easyfinance.accounts.application.response.AccountMemberResponse;
import com.easyfinance.accounts.application.response.AccountResponse;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.entrypoint.rest.dto.AccountMemberResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.AccountResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.AddAccountMemberRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.ChangeAccountMemberRoleRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.CreateAccountRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.UpdateAccountRequest;

public final class AccountRestMapper {

    private AccountRestMapper() {
    }

    public static CreateAccountCommand toCommand(CreateAccountRequest request) {
        return new CreateAccountCommand(request.name(), request.description());
    }

    public static UpdateAccountCommand toCommand(Long accountId, UpdateAccountRequest request) {
        return new UpdateAccountCommand(accountId, request.name(), request.description());
    }

    public static AddAccountMemberCommand toCommand(Long accountId, AddAccountMemberRequest request) {
        return new AddAccountMemberCommand(accountId, request.email(), AccountParticipantRole.valueOf(request.role().name()));
    }

    public static ChangeAccountMemberRoleCommand toCommand(Long accountId, Long participantId, ChangeAccountMemberRoleRequest request) {
        return new ChangeAccountMemberRoleCommand(accountId, participantId, AccountParticipantRole.valueOf(request.role().name()));
    }

    public static AccountResponseDto toDto(AccountResponse response) {
        return new AccountResponseDto(
                response.id(),
                response.name(),
                response.description(),
                response.status(),
                response.currentUserRole(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public static AccountMemberResponseDto toDto(AccountMemberResponse response) {
        return new AccountMemberResponseDto(
                response.participantId(),
                response.email(),
                response.displayName(),
                response.role(),
                response.status(),
                response.joinedAt()
        );
    }

    public static PageResponseDto<AccountResponseDto> toDto(PageResponse<AccountResponse> response) {
        return new PageResponseDto<>(
                response.content().stream().map(AccountRestMapper::toDto).toList(),
                response.page(),
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
    }
}
