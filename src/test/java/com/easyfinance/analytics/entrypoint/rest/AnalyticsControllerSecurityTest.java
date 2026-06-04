package com.easyfinance.analytics.entrypoint.rest;

import com.easyfinance.analytics.application.port.in.GetBudgetSummaryPort;
import com.easyfinance.analytics.application.port.in.GetBudgetVsExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetCashflowPort;
import com.easyfinance.analytics.application.port.in.GetCashflowSummaryPort;
import com.easyfinance.analytics.application.port.in.GetDebtSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpenseSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodTypePort;
import com.easyfinance.analytics.application.port.in.GetIncomesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetMonthlySummaryPort;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = AnalyticsControllerSecurityTest.TestConfig.class)
class AnalyticsControllerSecurityTest {

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
    void analyticsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/analytics/budget-vs-expenses-by-category?year=2026&month=5"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/monthly-summary?year=2026&month=5").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({AnalyticsController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean GetMonthlySummaryPort getMonthlySummaryPort() { return mock(GetMonthlySummaryPort.class); }
        @Bean GetCashflowSummaryPort getCashflowSummaryPort() { return mock(GetCashflowSummaryPort.class); }
        @Bean GetExpenseSummaryPort getExpenseSummaryPort() { return mock(GetExpenseSummaryPort.class); }
        @Bean GetCashflowPort getCashflowPort() { return mock(GetCashflowPort.class); }
        @Bean GetExpensesByCategoryPort getExpensesByCategoryPort() { return mock(GetExpensesByCategoryPort.class); }
        @Bean GetExpensesByPaymentMethodPort getExpensesByPaymentMethodPort() { return mock(GetExpensesByPaymentMethodPort.class); }
        @Bean GetExpensesByPaymentMethodTypePort getExpensesByPaymentMethodTypePort() { return mock(GetExpensesByPaymentMethodTypePort.class); }
        @Bean GetIncomesByCategoryPort getIncomesByCategoryPort() { return mock(GetIncomesByCategoryPort.class); }
        @Bean GetDebtSummaryPort getDebtSummaryPort() { return mock(GetDebtSummaryPort.class); }
        @Bean GetBudgetSummaryPort getBudgetSummaryPort() { return mock(GetBudgetSummaryPort.class); }
        @Bean GetBudgetVsExpensesByCategoryPort getBudgetVsExpensesByCategoryPort() { return mock(GetBudgetVsExpensesByCategoryPort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
