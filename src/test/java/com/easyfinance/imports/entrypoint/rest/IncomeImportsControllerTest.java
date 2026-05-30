package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GenerateIncomeImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportIncomePort;
import com.easyfinance.imports.application.response.IncomeImportResponse;
import com.easyfinance.imports.application.response.IncomeImportRowResponse;
import com.easyfinance.imports.application.response.IncomeImportTemplateResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncomeImportsControllerTest {

    private final GenerateIncomeImportTemplatePort generateIncomeImportTemplatePort = mock(GenerateIncomeImportTemplatePort.class);
    private final ImportIncomePort importIncomePort = mock(ImportIncomePort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IncomeImportsController(generateIncomeImportTemplatePort, importIncomePort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void templateReturnsExcelAttachment() throws Exception {
        when(generateIncomeImportTemplatePort.generate(1L)).thenReturn(new IncomeImportTemplateResponse(
                "easy-finance-income-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        ));

        mockMvc.perform(get("/api/v1/accounts/1/imports/incomes/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"easy-finance-income-import-template.xlsx\""));

        verify(generateIncomeImportTemplatePort).generate(1L);
    }

    @Test
    void importMultipartDelegates() throws Exception {
        when(importIncomePort.importIncomes(any())).thenReturn(new IncomeImportResponse(
                2,
                List.of(
                        new IncomeImportRowResponse(2, true, 101L, List.of()),
                        new IncomeImportRowResponse(3, true, 102L, List.of())
                )
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "incomes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/incomes").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.rows[0].createdIncomeId").value(101));
    }

    @Test
    void importWithoutFileReturnsStableError() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/incomes"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }
}

