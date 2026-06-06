package com.easyfinance.debts.entrypoint.rest;

import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.port.in.CancelDebtPort;
import com.easyfinance.debts.application.port.in.CreateManualDebtPort;
import com.easyfinance.debts.application.port.in.GetDebtPort;
import com.easyfinance.debts.application.port.in.ListDebtsPort;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DebtsControllerTest {

    private final CreateManualDebtPort createManualDebtPort = mock(CreateManualDebtPort.class);
    private final ListDebtsPort listDebtsPort = mock(ListDebtsPort.class);
    private final GetDebtPort getDebtPort = mock(GetDebtPort.class);
    private final CancelDebtPort cancelDebtPort = mock(CancelDebtPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DebtsController(createManualDebtPort, listDebtsPort, getDebtPort, cancelDebtPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void debtEndpointsDelegate() throws Exception {
        when(createManualDebtPort.createManualDebt(any())).thenReturn(debt());
        when(listDebtsPort.listDebts(any())).thenReturn(new PageResponse<>(List.of(debt()), 0, 20, 1, 1));
        when(getDebtPort.getDebt(1L, 5L)).thenReturn(debt());

        mockMvc.perform(post("/api/v1/accounts/1/debts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Loan","totalAmount":100000,"startDate":"2026-05-11"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("MANUAL"));

        mockMvc.perform(get("/api/v1/accounts/1/debts?state=ACTIVE&sourceType=MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Loan"));

        mockMvc.perform(get("/api/v1/accounts/1/debts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/accounts/1/debts/5/cancel"))
                .andExpect(status().isNoContent());
        verify(cancelDebtPort).cancelDebt(1L, 5L);
    }

    @Test
    void validatesCreateManualDebtRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/debts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","totalAmount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createManualDebtAcceptsOptionalParticipantId() throws Exception {
        when(createManualDebtPort.createManualDebt(any())).thenReturn(debt());

        mockMvc.perform(post("/api/v1/accounts/1/debts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Loan","participantId":20,"totalAmount":100000,"startDate":"2026-05-11"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateManualDebtCommand> captor = ArgumentCaptor.forClass(CreateManualDebtCommand.class);
        verify(createManualDebtPort).createManualDebt(captor.capture());
        assertThat(captor.getValue().participantId()).isEqualTo(20L);
    }

    private static DebtResponse debt() {
        return new DebtResponse(5L, 1L, 10L, null, "MANUAL", "Loan", null, new BigDecimal("100000.00"), new BigDecimal("100000.00"), "COP", new BigDecimal("100000.00"), "COP", null, null, null, LocalDate.of(2026, 5, 11), null, "ACTIVE", null, Instant.now(), Instant.now());
    }
}
