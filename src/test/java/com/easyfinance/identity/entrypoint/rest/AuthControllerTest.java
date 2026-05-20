package com.easyfinance.identity.entrypoint.rest;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final RegisterUserPort registerUserPort = mock(RegisterUserPort.class);
    private final LoginPort loginPort = mock(LoginPort.class);
    private final GetCurrentUserPort getCurrentUserPort = mock(GetCurrentUserPort.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(registerUserPort, loginPort, getCurrentUserPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturnsCreatedTokenAndUser() throws Exception {
        when(registerUserPort.register(any())).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("jane@example.com", "abc12345", "Jane Doe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.participantId").value(10))
                .andExpect(jsonPath("$.user.globalRoles[*]", containsInAnyOrder("USER")));
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        when(loginPort.login(any())).thenReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jane@example.com","password":"abc12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(getCurrentUserPort.getCurrentUser()).thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.participantId").value(10));
    }

    @Test
    void registerValidationErrorsUseStandardFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","password":"short","fullName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static AuthTokenResponse tokenResponse() {
        return new AuthTokenResponse("token", "Bearer", 3600L, userResponse());
    }

    private static AuthenticatedUserResponse userResponse() {
        return new AuthenticatedUserResponse(1L, 10L, "jane@example.com", "Jane Doe", Set.of("USER"));
    }

    private record RequestBody(String email, String password, String fullName) {
    }
}
