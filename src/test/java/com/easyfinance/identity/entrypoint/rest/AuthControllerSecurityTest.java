package com.easyfinance.identity.entrypoint.rest;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = AuthControllerSecurityTest.TestConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RegisterUserPort registerUserPort;

    @Autowired
    private LoginPort loginPort;

    @Autowired
    private GetCurrentUserPort getCurrentUserPort;

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
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meWithInvalidTokenReturnsUnauthorized() throws Exception {
        when(jwtTokenService.validate("bad-token")).thenThrow(new JwtAuthenticationException("INVALID_TOKEN", "Invalid token."));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void registerRemainsPublic() throws Exception {
        when(registerUserPort.register(any())).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"abc12345","fullName":"Jane Doe"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    void loginRemainsPublic() throws Exception {
        when(loginPort.login(any())).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"abc12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"));
    }

    @Test
    void corsPreflightFromAngularLocalhostIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    private static AuthTokenResponse tokenResponse() {
        return new AuthTokenResponse(
                "token",
                "Bearer",
                3600L,
                new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Doe", Set.of("USER"))
        );
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({AuthController.class, SecurityConfig.class, JwtAuthenticationFilter.class, RestSecurityExceptionHandler.class})
    static class TestConfig {

        @Bean
        RegisterUserPort registerUserPort() {
            return mock(RegisterUserPort.class);
        }

        @Bean
        LoginPort loginPort() {
            return mock(LoginPort.class);
        }

        @Bean
        GetCurrentUserPort getCurrentUserPort() {
            return mock(GetCurrentUserPort.class);
        }

        @Bean
        JwtTokenService jwtTokenService() {
            return mock(JwtTokenService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
