package com.easyfinance.catalogs.entrypoint.rest;

import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.DeactivateCategoryPort;
import com.easyfinance.catalogs.application.port.in.DeactivatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.port.in.GetPaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.ListCategoriesPort;
import com.easyfinance.catalogs.application.port.in.ListPaymentMethodsPort;
import com.easyfinance.catalogs.application.port.in.UpdateCategoryPort;
import com.easyfinance.catalogs.application.port.in.UpdatePaymentMethodPort;
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

@SpringJUnitWebConfig(classes = CatalogsControllerSecurityTest.TestConfig.class)
class CatalogsControllerSecurityTest {

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
    void catalogsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/v1/accounts/1/payment-methods").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/accounts/1/categories").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({CategoriesController.class, PaymentMethodsController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean CreateCategoryPort createCategoryPort() { return mock(CreateCategoryPort.class); }
        @Bean ListCategoriesPort listCategoriesPort() { return mock(ListCategoriesPort.class); }
        @Bean GetCategoryPort getCategoryPort() { return mock(GetCategoryPort.class); }
        @Bean UpdateCategoryPort updateCategoryPort() { return mock(UpdateCategoryPort.class); }
        @Bean DeactivateCategoryPort deactivateCategoryPort() { return mock(DeactivateCategoryPort.class); }
        @Bean CreatePaymentMethodPort createPaymentMethodPort() { return mock(CreatePaymentMethodPort.class); }
        @Bean ListPaymentMethodsPort listPaymentMethodsPort() { return mock(ListPaymentMethodsPort.class); }
        @Bean GetPaymentMethodPort getPaymentMethodPort() { return mock(GetPaymentMethodPort.class); }
        @Bean UpdatePaymentMethodPort updatePaymentMethodPort() { return mock(UpdatePaymentMethodPort.class); }
        @Bean DeactivatePaymentMethodPort deactivatePaymentMethodPort() { return mock(DeactivatePaymentMethodPort.class); }
        @Bean JwtTokenService jwtTokenService() { return mock(JwtTokenService.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
}
