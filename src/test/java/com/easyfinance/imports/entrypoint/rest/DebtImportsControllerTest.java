package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GenerateDebtImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportDebtPort;
import com.easyfinance.imports.application.port.in.PreviewDebtImportPort;
import com.easyfinance.imports.application.response.DebtImportResponse;
import com.easyfinance.imports.application.response.DebtImportRowResponse;
import com.easyfinance.imports.application.response.DebtImportTemplateResponse;
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

class DebtImportsControllerTest {

    private final GenerateDebtImportTemplatePort generateDebtImportTemplatePort = mock(GenerateDebtImportTemplatePort.class);
    private final ImportDebtPort importDebtPort = mock(ImportDebtPort.class);
    private final PreviewDebtImportPort previewDebtImportPort = mock(PreviewDebtImportPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DebtImportsController(generateDebtImportTemplatePort, importDebtPort, previewDebtImportPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void templateReturnsExcelAttachment() throws Exception {
        when(generateDebtImportTemplatePort.generate(1L)).thenReturn(new DebtImportTemplateResponse(
                "easy-finance-debt-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        ));

        mockMvc.perform(get("/api/v1/accounts/1/imports/debts/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"easy-finance-debt-import-template.xlsx\""));

        verify(generateDebtImportTemplatePort).generate(1L);
    }

    @Test
    void previewMultipartReturnsParsedRowData() throws Exception {
        when(previewDebtImportPort.previewDebts(any())).thenReturn(new DebtImportResponse(
                0,
                List.of(row(2, null))
        ));
        MockMultipartFile file = file("debts.xlsx");

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/debts/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.rows[0].name").value("Prestamo carro"))
                .andExpect(jsonPath("$.rows[0].totalAmount").value(5000000))
                .andExpect(jsonPath("$.rows[0].remainingBalance").value(3000000))
                .andExpect(jsonPath("$.rows[0].participantLabel").value("Usuario Actual <user@example.com>"))
                .andExpect(jsonPath("$.rows[0].participantId").value(10));
    }

    @Test
    void importMultipartDelegates() throws Exception {
        when(importDebtPort.importDebts(any())).thenReturn(new DebtImportResponse(
                2,
                List.of(row(2, 101L), row(3, 102L))
        ));
        MockMultipartFile file = file("debts.xlsx");

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/debts").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.rows[0].createdDebtId").value(101))
                .andExpect(jsonPath("$.rows[0].name").value("Prestamo carro"));
    }

    @Test
    void importWithoutFileReturnsStableError() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/debts"))
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

    private static DebtImportRowResponse row(int rowNumber, Long createdId) {
        return new DebtImportRowResponse(
                rowNumber,
                "Prestamo carro",
                null,
                new BigDecimal("5000000"),
                new BigDecimal("3000000"),
                null,
                null,
                LocalDate.of(2026, 5, 1),
                null,
                "Usuario Actual <user@example.com>",
                10L,
                null,
                true,
                createdId,
                List.of()
        );
    }
}
