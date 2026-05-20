package com.easyfinance.budgets.entrypoint.rest;

import com.easyfinance.budgets.application.port.in.CreateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DeactivateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DuplicateBudgetPort;
import com.easyfinance.budgets.application.port.in.GetBudgetPort;
import com.easyfinance.budgets.application.port.in.ListBudgetsPort;
import com.easyfinance.budgets.application.port.in.UpdateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.UpsertBudgetPort;
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

@SpringJUnitWebConfig(classes = BudgetsControllerSecurityTest.TestConfig.class)
class BudgetsControllerSecurityTest {

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
    void budgetEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/budgets/2026/5"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicateBudgetRequiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/budgets/2026/5/duplicate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"targetYear\":2026,\"targetMonth\":6}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts/1/budgets/2026/5").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void publicDocumentationEndpointIsNotBlockedBySecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({BudgetsController.class, SubBudgetsController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean UpsertBudgetPort upsertBudgetPort() { return mock(UpsertBudgetPort.class); }
        @Bean GetBudgetPort getBudgetPort() { return mock(GetBudgetPort.class); }
        @Bean ListBudgetsPort listBudgetsPort() { return mock(ListBudgetsPort.class); }
        @Bean DuplicateBudgetPort duplicateBudgetPort() { return mock(DuplicateBudgetPort.class); }
        @Bean CreateSubBudgetPort createSubBudgetPort() { return mock(CreateSubBudgetPort.class); }
        @Bean UpdateSubBudgetPort updateSubBudgetPort() { return mock(UpdateSubBudgetPort.class); }
        @Bean DeactivateSubBudgetPort deactivateSubBudgetPort() { return mock(DeactivateSubBudgetPort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
