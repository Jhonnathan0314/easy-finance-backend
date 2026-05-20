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
import com.easyfinance.shared.infrastructure.security.JwtAuthenticationException;
import com.easyfinance.shared.infrastructure.security.JwtAuthenticationFilter;
import com.easyfinance.shared.infrastructure.security.JwtTokenService;
import com.easyfinance.shared.infrastructure.security.RestSecurityExceptionHandler;
import com.easyfinance.shared.infrastructure.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = AccountsControllerSecurityTest.TestConfig.class)
class AccountsControllerSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenService jwtTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void accountsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/v1/accounts").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({AccountsController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean CreateAccountPort createAccountPort() { return mock(CreateAccountPort.class); }
        @Bean ListAccountsPort listAccountsPort() { return mock(ListAccountsPort.class); }
        @Bean GetAccountPort getAccountPort() { return mock(GetAccountPort.class); }
        @Bean UpdateAccountPort updateAccountPort() { return mock(UpdateAccountPort.class); }
        @Bean ArchiveAccountPort archiveAccountPort() { return mock(ArchiveAccountPort.class); }
        @Bean ListAccountMembersPort listAccountMembersPort() { return mock(ListAccountMembersPort.class); }
        @Bean AddAccountMemberPort addAccountMemberPort() { return mock(AddAccountMemberPort.class); }
        @Bean ChangeAccountMemberRolePort changeAccountMemberRolePort() { return mock(ChangeAccountMemberRolePort.class); }
        @Bean RemoveAccountMemberPort removeAccountMemberPort() { return mock(RemoveAccountMemberPort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
