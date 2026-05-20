package com.easyfinance.income.entrypoint.rest;

import com.easyfinance.income.application.port.in.CancelIncomePort;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.income.application.port.in.DuplicateIncomePort;
import com.easyfinance.income.application.port.in.GetIncomePort;
import com.easyfinance.income.application.port.in.ListIncomesPort;
import com.easyfinance.income.application.port.in.UpdateIncomePort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.application.response.IncomeResponse;
import com.easyfinance.income.application.response.PageResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncomesControllerTest {

    private final CreateIncomePort createIncomePort = mock(CreateIncomePort.class);
    private final ListIncomesPort listIncomesPort = mock(ListIncomesPort.class);
    private final GetIncomePort getIncomePort = mock(GetIncomePort.class);
    private final UpdateIncomePort updateIncomePort = mock(UpdateIncomePort.class);
    private final CancelIncomePort cancelIncomePort = mock(CancelIncomePort.class);
    private final DuplicateIncomePort duplicateIncomePort = mock(DuplicateIncomePort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IncomesController(createIncomePort, listIncomesPort, getIncomePort, updateIncomePort, cancelIncomePort, duplicateIncomePort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void incomeEndpointsDelegate() throws Exception {
        when(createIncomePort.createIncome(any())).thenReturn(income());
        when(listIncomesPort.listIncomes(any())).thenReturn(new PageResponse<>(List.of(income()), 0, 20, 1, 1));
        when(getIncomePort.getIncome(1L, 5L)).thenReturn(income());
        when(updateIncomePort.updateIncome(any())).thenReturn(income());
        when(duplicateIncomePort.duplicateIncome(any())).thenReturn(income());

        mockMvc.perform(post("/api/v1/accounts/1/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"description":"Salary","amount":2500000,"incomeDate":"2026-05-10"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/accounts/1/incomes?from=2026-05-01&to=2026-05-31&status=ACTIVE&sort=incomeDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Salary"));

        mockMvc.perform(get("/api/v1/accounts/1/incomes/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2500000.00));

        mockMvc.perform(put("/api/v1/accounts/1/incomes/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"description":"Updated salary","amount":2600000,"incomeDate":"2026-05-11"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/accounts/1/incomes/5/cancel"))
                .andExpect(status().isNoContent());
        verify(cancelIncomePort).cancelIncome(1L, 5L);

        mockMvc.perform(post("/api/v1/accounts/1/incomes/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomeDate":"2026-06-30","amount":5200000,"description":"June salary"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void validatesCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"description":"","amount":0,"incomeDate":"2026-05-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void validatesDuplicateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/incomes/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listReceivesTrimmedSearchParam() throws Exception {
        when(listIncomesPort.listIncomes(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/1/incomes?search= Nomina "))
                .andExpect(status().isOk());

        ArgumentCaptor<ListIncomesQuery> captor = ArgumentCaptor.forClass(ListIncomesQuery.class);
        verify(listIncomesPort).listIncomes(captor.capture());
        assertThat(captor.getValue().search()).isEqualTo("Nomina");
    }

    @Test
    void listIgnoresBlankSearchParam() throws Exception {
        when(listIncomesPort.listIncomes(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/1/incomes?search=   "))
                .andExpect(status().isOk());

        ArgumentCaptor<ListIncomesQuery> captor = ArgumentCaptor.forClass(ListIncomesQuery.class);
        verify(listIncomesPort).listIncomes(captor.capture());
        assertThat(captor.getValue().search()).isNull();
    }

    private static IncomeResponse income() {
        return new IncomeResponse(
                5L,
                1L,
                2L,
                10L,
                "Salary",
                new BigDecimal("2500000.00"),
                "COP",
                LocalDate.of(2026, 5, 10),
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
    }
}
