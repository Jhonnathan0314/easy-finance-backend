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
import com.easyfinance.accounts.application.response.AccountMemberResponse;
import com.easyfinance.accounts.application.response.AccountResponse;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountsControllerTest {

    private final CreateAccountPort createAccountPort = mock(CreateAccountPort.class);
    private final ListAccountsPort listAccountsPort = mock(ListAccountsPort.class);
    private final GetAccountPort getAccountPort = mock(GetAccountPort.class);
    private final UpdateAccountPort updateAccountPort = mock(UpdateAccountPort.class);
    private final ArchiveAccountPort archiveAccountPort = mock(ArchiveAccountPort.class);
    private final ListAccountMembersPort listAccountMembersPort = mock(ListAccountMembersPort.class);
    private final AddAccountMemberPort addAccountMemberPort = mock(AddAccountMemberPort.class);
    private final ChangeAccountMemberRolePort changeAccountMemberRolePort = mock(ChangeAccountMemberRolePort.class);
    private final RemoveAccountMemberPort removeAccountMemberPort = mock(RemoveAccountMemberPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AccountsController(
                        createAccountPort,
                        listAccountsPort,
                        getAccountPort,
                        updateAccountPort,
                        archiveAccountPort,
                        listAccountMembersPort,
                        addAccountMemberPort,
                        changeAccountMemberRolePort,
                        removeAccountMemberPort
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsAccount() throws Exception {
        when(createAccountPort.createAccount(any())).thenReturn(account());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Home","description":"Family"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currentUserRole").value("ACCOUNT_ADMIN"));
    }

    @Test
    void listsAccounts() throws Exception {
        when(listAccountsPort.listMyAccounts(any())).thenReturn(new PageResponse<>(List.of(account()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Home"));
    }

    @Test
    void getsUpdatesAndArchivesAccount() throws Exception {
        when(getAccountPort.getAccount(1L)).thenReturn(account());
        when(updateAccountPort.updateAccount(any())).thenReturn(account());
        when(archiveAccountPort.archiveAccount(1L)).thenReturn(new AccountResponse(1L, "Home", "Family", "ARCHIVED", "ACCOUNT_ADMIN", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home"));

        mockMvc.perform(put("/api/v1/accounts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Home","description":"Family"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home"));

        mockMvc.perform(patch("/api/v1/accounts/1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void managesMembers() throws Exception {
        when(listAccountMembersPort.listMembers(1L)).thenReturn(List.of(member()));
        when(addAccountMemberPort.addMember(any())).thenReturn(member());
        when(changeAccountMemberRolePort.changeMemberRole(any())).thenReturn(new AccountMemberResponse(20L, "member@example.com", "Member", "ACCOUNT_ADMIN", "ACTIVE", Instant.now()));

        mockMvc.perform(get("/api/v1/accounts/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantId").value(20));

        mockMvc.perform(post("/api/v1/accounts/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","role":"ACCOUNT_MEMBER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ACCOUNT_MEMBER"));

        mockMvc.perform(patch("/api/v1/accounts/1/members/20/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ACCOUNT_ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ACCOUNT_ADMIN"));

        mockMvc.perform(delete("/api/v1/accounts/1/members/20"))
                .andExpect(status().isNoContent());
        verify(removeAccountMemberPort).removeMember(1L, 20L);
    }

    @Test
    void validatesCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static AccountResponse account() {
        return new AccountResponse(1L, "Home", "Family", "ACTIVE", "ACCOUNT_ADMIN", Instant.now(), Instant.now());
    }

    private static AccountMemberResponse member() {
        return new AccountMemberResponse(20L, "member@example.com", "Member", "ACCOUNT_MEMBER", "ACTIVE", Instant.now());
    }
}
