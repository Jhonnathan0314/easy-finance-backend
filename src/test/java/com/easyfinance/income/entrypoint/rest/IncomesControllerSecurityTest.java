package com.easyfinance.income.entrypoint.rest;

import com.easyfinance.income.application.port.in.CancelIncomePort;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.income.application.port.in.DuplicateIncomePort;
import com.easyfinance.income.application.port.in.GetIncomePort;
import com.easyfinance.income.application.port.in.ListIncomesPort;
import com.easyfinance.income.application.port.in.UpdateIncomePort;
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

@SpringJUnitWebConfig(classes = IncomesControllerSecurityTest.TestConfig.class)
class IncomesControllerSecurityTest {

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
    void incomesRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/incomes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicateIncomeRequiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/incomes/5/duplicate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomeDate":"2026-06-30"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts/1/incomes").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({IncomesController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean CreateIncomePort createIncomePort() { return mock(CreateIncomePort.class); }
        @Bean ListIncomesPort listIncomesPort() { return mock(ListIncomesPort.class); }
        @Bean GetIncomePort getIncomePort() { return mock(GetIncomePort.class); }
        @Bean UpdateIncomePort updateIncomePort() { return mock(UpdateIncomePort.class); }
        @Bean CancelIncomePort cancelIncomePort() { return mock(CancelIncomePort.class); }
        @Bean DuplicateIncomePort duplicateIncomePort() { return mock(DuplicateIncomePort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
