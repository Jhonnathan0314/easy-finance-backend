package com.easyfinance.accounts.entrypoint.rest;

import com.easyfinance.accounts.application.port.in.AddAccountMemberPort;
import com.easyfinance.accounts.application.port.in.ArchiveAccountPort;
import com.easyfinance.accounts.application.port.in.ChangeAccountMemberRolePort;
import com.easyfinance.accounts.application.port.in.CreateAccountPort;
import com.easyfinance.accounts.application.port.in.GetAccountPort;
import com.easyfinance.accounts.application.port.in.ListAccountMembersPort;
import com.easyfinance.accounts.application.port.in.ListAccountsPort;
import com.easyfinance.accounts.application.port.in.RemoveAccountMemberPort;
import com.easyfinance.accounts.application.port.in.UpdateAccountPort;
import com.easyfinance.accounts.application.query.ListAccountsQuery;
import com.easyfinance.accounts.entrypoint.rest.dto.AccountMemberResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.AccountResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.AddAccountMemberRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.ChangeAccountMemberRoleRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.CreateAccountRequest;
import com.easyfinance.accounts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.accounts.entrypoint.rest.dto.UpdateAccountRequest;
import com.easyfinance.accounts.entrypoint.rest.mapper.AccountRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

    private final CreateAccountPort createAccountPort;
    private final ListAccountsPort listAccountsPort;
    private final GetAccountPort getAccountPort;
    private final UpdateAccountPort updateAccountPort;
    private final ArchiveAccountPort archiveAccountPort;
    private final ListAccountMembersPort listAccountMembersPort;
    private final AddAccountMemberPort addAccountMemberPort;
    private final ChangeAccountMemberRolePort changeAccountMemberRolePort;
    private final RemoveAccountMemberPort removeAccountMemberPort;

    public AccountsController(
            CreateAccountPort createAccountPort,
            ListAccountsPort listAccountsPort,
            GetAccountPort getAccountPort,
            UpdateAccountPort updateAccountPort,
            ArchiveAccountPort archiveAccountPort,
            ListAccountMembersPort listAccountMembersPort,
            AddAccountMemberPort addAccountMemberPort,
            ChangeAccountMemberRolePort changeAccountMemberRolePort,
            RemoveAccountMemberPort removeAccountMemberPort
    ) {
        this.createAccountPort = createAccountPort;
        this.listAccountsPort = listAccountsPort;
        this.getAccountPort = getAccountPort;
        this.updateAccountPort = updateAccountPort;
        this.archiveAccountPort = archiveAccountPort;
        this.listAccountMembersPort = listAccountMembersPort;
        this.addAccountMemberPort = addAccountMemberPort;
        this.changeAccountMemberRolePort = changeAccountMemberRolePort;
        this.removeAccountMemberPort = removeAccountMemberPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDto create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountRestMapper.toDto(createAccountPort.createAccount(AccountRestMapper.toCommand(request)));
    }

    @GetMapping
    public PageResponseDto<AccountResponseDto> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return AccountRestMapper.toDto(listAccountsPort.listMyAccounts(new ListAccountsQuery(PageQuery.of(page, size))));
    }

    @GetMapping("/{accountId}")
    public AccountResponseDto get(@PathVariable Long accountId) {
        return AccountRestMapper.toDto(getAccountPort.getAccount(accountId));
    }

    @PutMapping("/{accountId}")
    public AccountResponseDto update(@PathVariable Long accountId, @Valid @RequestBody UpdateAccountRequest request) {
        return AccountRestMapper.toDto(updateAccountPort.updateAccount(AccountRestMapper.toCommand(accountId, request)));
    }

    @PatchMapping("/{accountId}/archive")
    public AccountResponseDto archive(@PathVariable Long accountId) {
        return AccountRestMapper.toDto(archiveAccountPort.archiveAccount(accountId));
    }

    @GetMapping("/{accountId}/members")
    public List<AccountMemberResponseDto> listMembers(@PathVariable Long accountId) {
        return listAccountMembersPort.listMembers(accountId).stream().map(AccountRestMapper::toDto).toList();
    }

    @PostMapping("/{accountId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountMemberResponseDto addMember(@PathVariable Long accountId, @Valid @RequestBody AddAccountMemberRequest request) {
        return AccountRestMapper.toDto(addAccountMemberPort.addMember(AccountRestMapper.toCommand(accountId, request)));
    }

    @PatchMapping("/{accountId}/members/{participantId}/role")
    public AccountMemberResponseDto changeRole(
            @PathVariable Long accountId,
            @PathVariable Long participantId,
            @Valid @RequestBody ChangeAccountMemberRoleRequest request
    ) {
        return AccountRestMapper.toDto(changeAccountMemberRolePort.changeMemberRole(AccountRestMapper.toCommand(accountId, participantId, request)));
    }

    @DeleteMapping("/{accountId}/members/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long accountId, @PathVariable Long participantId) {
        removeAccountMemberPort.removeMember(accountId, participantId);
    }
}
