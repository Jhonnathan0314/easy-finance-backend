package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GenerateIncomeImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportIncomePort;
import com.easyfinance.imports.application.port.in.PreviewIncomeImportPort;
import com.easyfinance.imports.application.response.IncomeImportResponse;
import com.easyfinance.imports.application.response.IncomeImportRowResponse;
import com.easyfinance.imports.application.response.IncomeImportTemplateResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final PreviewIncomeImportPort previewIncomeImportPort = mock(PreviewIncomeImportPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IncomeImportsController(generateIncomeImportTemplatePort, importIncomePort, previewIncomeImportPort))
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
    void previewMultipartReturnsParsedRowData() throws Exception {
        when(previewIncomeImportPort.previewIncomes(any())).thenReturn(new IncomeImportResponse(
                0,
                List.of(row(2, null))
        ));
        MockMultipartFile file = file("incomes.xlsx");

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/incomes/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.rows[0].incomeDate").value("2026-05-20"))
                .andExpect(jsonPath("$.rows[0].description").value("Salario"))
                .andExpect(jsonPath("$.rows[0].categoryName").value("Nomina"))
                .andExpect(jsonPath("$.rows[0].categoryId").value(7))
                .andExpect(jsonPath("$.rows[0].participantLabel").value("Usuario Actual <user@example.com>"))
                .andExpect(jsonPath("$.rows[0].participantId").value(10))
                .andExpect(jsonPath("$.rows[0].amount").value(1500000));
    }

    @Test
    void importMultipartDelegates() throws Exception {
        when(importIncomePort.importIncomes(any())).thenReturn(new IncomeImportResponse(
                2,
                List.of(
                        row(2, 101L),
                        row(3, 102L)
                )
        ));
        MockMultipartFile file = file("incomes.xlsx");

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/incomes").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.rows[0].createdIncomeId").value(101))
                .andExpect(jsonPath("$.rows[0].description").value("Salario"));
    }

    @Test
    void importWithoutFileReturnsStableError() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/incomes"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }

    private static MockMultipartFile file(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );
    }

    private static IncomeImportRowResponse row(int rowNumber, Long createdId) {
        return new IncomeImportRowResponse(
                rowNumber,
                LocalDate.of(2026, 5, 20),
                "Salario",
                "Nomina",
                7L,
                "Usuario Actual <user@example.com>",
                10L,
                new BigDecimal("1500000"),
                true,
                createdId,
                List.of()
        );
    }
}
