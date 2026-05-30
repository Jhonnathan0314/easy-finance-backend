package com.easyfinance.budgets.entrypoint.rest;

import com.easyfinance.budgets.application.port.in.DuplicateBudgetPort;
import com.easyfinance.budgets.application.port.in.CreateAnnualBudgetPort;
import com.easyfinance.budgets.application.port.in.GetBudgetPort;
import com.easyfinance.budgets.application.port.in.ListBudgetsPort;
import com.easyfinance.budgets.application.port.in.UpsertBudgetPort;
import com.easyfinance.budgets.application.response.AnnualBudgetResponse;
import com.easyfinance.budgets.application.response.BudgetDetailResponse;
import com.easyfinance.budgets.application.response.BudgetResponse;
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetsControllerTest {

    private final UpsertBudgetPort upsertBudgetPort = mock(UpsertBudgetPort.class);
    private final GetBudgetPort getBudgetPort = mock(GetBudgetPort.class);
    private final ListBudgetsPort listBudgetsPort = mock(ListBudgetsPort.class);
    private final DuplicateBudgetPort duplicateBudgetPort = mock(DuplicateBudgetPort.class);
    private final CreateAnnualBudgetPort createAnnualBudgetPort = mock(CreateAnnualBudgetPort.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BudgetsController(upsertBudgetPort, getBudgetPort, listBudgetsPort, duplicateBudgetPort, createAnnualBudgetPort))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void upsertBudgetReturnsBudget() throws Exception {
        when(upsertBudgetPort.upsertBudget(any())).thenReturn(budget());

        mockMvc.perform(put("/api/v1/accounts/1/budgets/2026/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"May\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getBudgetReturnsDetails() throws Exception {
        when(getBudgetPort.getBudget(1L, 2026, 5)).thenReturn(new BudgetDetailResponse(budget(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/accounts/1/budgets/2026/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget.id").value(10));
    }

    @Test
    void listBudgetsReturnsPage() throws Exception {
        when(listBudgetsPort.listBudgets(any())).thenReturn(new PageResponse<>(List.of(budget()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts/1/budgets?year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void duplicateBudgetReturnsTargetDetails() throws Exception {
        when(duplicateBudgetPort.duplicateBudget(any())).thenReturn(new BudgetDetailResponse(
                new BudgetResponse(11L, 1L, 2026, 6, "June", "ACTIVE", Instant.now(), Instant.now()),
                List.of(),
                List.of()
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/accounts/1/budgets/2026/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetYear\":2026,\"targetMonth\":6,\"name\":\"June\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budget.id").value(11))
                .andExpect(jsonPath("$.budget.month").value(6));
    }

    @Test
    void duplicateBudgetValidatesBody() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/accounts/1/budgets/2026/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetYear\":2026,\"targetMonth\":13}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createAnnualBudgetReturnsCreatedBudgets() throws Exception {
        when(createAnnualBudgetPort.createAnnualBudget(any())).thenReturn(new AnnualBudgetResponse(
                1L,
                2026,
                List.of(
                        new BudgetResponse(11L, 1L, 2026, 1, "Presupuesto 2026", "ACTIVE", Instant.now(), Instant.now()),
                        new BudgetResponse(12L, 1L, 2026, 2, "Presupuesto 2026", "ACTIVE", Instant.now(), Instant.now())
                )
        ));

        mockMvc.perform(post("/api/v1/accounts/1/budgets/annual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2026,
                                  "name": "Presupuesto 2026",
                                  "subBudgets": [
                                    {"name":"Mercado","categoryId":1,"plannedAmount":800000}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.createdBudgets[0].month").value(1));
    }

    private static BudgetResponse budget() {
        return new BudgetResponse(10L, 1L, 2026, 5, "May", "ACTIVE", Instant.now(), Instant.now());
    }
}
