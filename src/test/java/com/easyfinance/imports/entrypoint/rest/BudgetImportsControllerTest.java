package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GenerateAnnualBudgetImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportAnnualBudgetPort;
import com.easyfinance.imports.application.response.AnnualBudgetImportResponse;
import com.easyfinance.imports.application.response.AnnualBudgetImportRowResponse;
import com.easyfinance.imports.application.response.AnnualBudgetImportTemplateResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetImportsControllerTest {

    private final GenerateAnnualBudgetImportTemplatePort generateTemplatePort = mock(GenerateAnnualBudgetImportTemplatePort.class);
    private final ImportAnnualBudgetPort importPort = mock(ImportAnnualBudgetPort.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BudgetImportsController(generateTemplatePort, importPort))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void downloadsTemplate() throws Exception {
        when(generateTemplatePort.generate(1L)).thenReturn(
                new AnnualBudgetImportTemplateResponse(
                        "easy-finance-annual-budget-import-template.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[]{1, 2, 3}
                )
        );

        mockMvc.perform(get("/api/v1/accounts/1/imports/budgets/annual/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("easy-finance-annual-budget-import-template.xlsx")));
    }

    @Test
    void importsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "budget.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2});
        when(importPort.importAnnualBudget(any())).thenReturn(new AnnualBudgetImportResponse(
                2026,
                12,
                24,
                List.of(new AnnualBudgetImportRowResponse(2, true, List.of(1, 2, 3), List.of()))
        ));

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/budgets/annual").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.createdBudgetsCount").value(12))
                .andExpect(jsonPath("$.rows[0].rowNumber").value(2));
    }
}

