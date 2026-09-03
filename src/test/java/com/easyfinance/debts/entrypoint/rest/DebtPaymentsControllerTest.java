package com.easyfinance.debts.entrypoint.rest;

import com.easyfinance.debts.application.port.in.GetDebtPaymentPort;
import com.easyfinance.debts.application.port.in.ListDebtPaymentsPort;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.application.response.RegisterDebtPaymentResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DebtPaymentsControllerTest {

    private final RegisterDebtPaymentPort registerDebtPaymentPort = mock(RegisterDebtPaymentPort.class);
    private final ListDebtPaymentsPort listDebtPaymentsPort = mock(ListDebtPaymentsPort.class);
    private final GetDebtPaymentPort getDebtPaymentPort = mock(GetDebtPaymentPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DebtPaymentsController(registerDebtPaymentPort, listDebtPaymentsPort, getDebtPaymentPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerPaymentDelegates() throws Exception {
        when(registerDebtPaymentPort.registerDebtPayment(any())).thenReturn(new RegisterDebtPaymentResponse(payment(), debt("ACTIVE", new BigDecimal("50000"))));

        mockMvc.perform(post("/api/v1/accounts/1/debts/5/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"INSTALLMENT","capitalAmount":50000,"paymentDate":"2026-05-11","notes":"First"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.amount").value(50000))
                .andExpect(jsonPath("$.debt.remainingAmount").value(50000));
        verify(registerDebtPaymentPort).registerDebtPayment(any());
    }

    @Test
    void listPaymentsDelegates() throws Exception {
        when(listDebtPaymentsPort.listDebtPayments(any())).thenReturn(new PageResponse<>(List.of(payment()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts/1/debts/5/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(50));
    }

    @Test
    void getPaymentDelegates() throws Exception {
        when(getDebtPaymentPort.getDebtPayment(1L, 5L, 50L)).thenReturn(payment());

        mockMvc.perform(get("/api/v1/accounts/1/debts/5/payments/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void registerPaymentValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/debts/5/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"INSTALLMENT","capitalAmount":0,"paymentDate":"2026-05-11"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPaymentValidatesNegativeInterestAmount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/debts/5/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentType":"INSTALLMENT","capitalAmount":50000,"interestAmount":-1,"paymentDate":"2026-05-11"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private static DebtPaymentResponse payment() {
        return new DebtPaymentResponse(50L, 1L, 5L, 10L, "INSTALLMENT", new BigDecimal("50000"), new BigDecimal("50000"), BigDecimal.ZERO, "COP", LocalDate.of(2026, 5, 11), "First", "ACTIVE", Instant.now(), Instant.now());
    }

    private static DebtResponse debt(String state, BigDecimal remaining) {
        return new DebtResponse(5L, 1L, 20L, null, "MANUAL", "Loan", null, new BigDecimal("100000"), new BigDecimal("100000"), "COP", remaining, "COP", null, null, null, LocalDate.of(2026, 5, 1), null, state, null, Instant.now(), Instant.now());
    }
}
