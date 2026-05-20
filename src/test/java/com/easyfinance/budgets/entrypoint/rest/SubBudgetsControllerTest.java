package com.easyfinance.budgets.entrypoint.rest;

import com.easyfinance.budgets.application.port.in.CreateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DeactivateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.UpdateSubBudgetPort;
import com.easyfinance.budgets.application.response.SubBudgetResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubBudgetsControllerTest {

    private final CreateSubBudgetPort createSubBudgetPort = mock(CreateSubBudgetPort.class);
    private final UpdateSubBudgetPort updateSubBudgetPort = mock(UpdateSubBudgetPort.class);
    private final DeactivateSubBudgetPort deactivateSubBudgetPort = mock(DeactivateSubBudgetPort.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SubBudgetsController(createSubBudgetPort, updateSubBudgetPort, deactivateSubBudgetPort))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void createSubBudgetReturnsCreated() throws Exception {
        when(createSubBudgetPort.createSubBudget(any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/accounts/1/budgets/10/sub-budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":7,\"name\":\"Food\",\"plannedAmount\":100000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void updateSubBudgetReturnsUpdated() throws Exception {
        when(updateSubBudgetPort.updateSubBudget(any())).thenReturn(response());

        mockMvc.perform(put("/api/v1/accounts/1/budgets/10/sub-budgets/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":7,\"name\":\"Food\",\"plannedAmount\":120000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void deleteSubBudgetReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/1/budgets/10/sub-budgets/20"))
                .andExpect(status().isNoContent());

        verify(deactivateSubBudgetPort).deactivateSubBudget(1L, 10L, 20L);
    }

    private static SubBudgetResponse response() {
        return new SubBudgetResponse(20L, 1L, 10L, 7L, null, "Food", new BigDecimal("100000.00"), "COP", BigDecimal.ZERO.setScale(2), "COP", "ACTIVE", "MANUAL", Instant.now(), Instant.now());
    }
}
