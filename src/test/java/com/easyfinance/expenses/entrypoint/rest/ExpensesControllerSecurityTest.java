package com.easyfinance.expenses.entrypoint.rest;

import com.easyfinance.expenses.application.port.in.CancelExpensePort;
import com.easyfinance.expenses.application.port.in.CreateExpensePort;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.expenses.application.port.in.DuplicateExpensePort;
import com.easyfinance.expenses.application.port.in.GetExpensePort;
import com.easyfinance.expenses.application.port.in.ListExpensesPort;
import com.easyfinance.expenses.application.port.in.UpdateExpensePort;
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

@SpringJUnitWebConfig(classes = ExpensesControllerSecurityTest.TestConfig.class)
class ExpensesControllerSecurityTest {

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
    void expensesRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicateExpenseRequiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/expenses/5/duplicate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"expenseDate":"2026-06-15"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts/1/expenses").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({ExpensesController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean CreateExpensePort createExpensePort() { return mock(CreateExpensePort.class); }
        @Bean CreateInstallmentExpensePort createInstallmentExpensePort() { return mock(CreateInstallmentExpensePort.class); }
        @Bean ListExpensesPort listExpensesPort() { return mock(ListExpensesPort.class); }
        @Bean GetExpensePort getExpensePort() { return mock(GetExpensePort.class); }
        @Bean UpdateExpensePort updateExpensePort() { return mock(UpdateExpensePort.class); }
        @Bean CancelExpensePort cancelExpensePort() { return mock(CancelExpensePort.class); }
        @Bean DuplicateExpensePort duplicateExpensePort() { return mock(DuplicateExpensePort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
