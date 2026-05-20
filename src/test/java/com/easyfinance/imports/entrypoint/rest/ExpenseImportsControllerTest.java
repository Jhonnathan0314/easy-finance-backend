package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.ConfirmExpenseImportPort;
import com.easyfinance.imports.application.port.in.GenerateExpenseImportTemplatePort;
import com.easyfinance.imports.application.port.in.GetExpenseImportBatchPort;
import com.easyfinance.imports.application.port.in.PreviewExpenseImportPort;
import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;
import com.easyfinance.imports.application.response.ExpenseImportRowResponse;
import com.easyfinance.imports.application.response.ExpenseImportTemplateResponse;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseImportsControllerTest {

    private final PreviewExpenseImportPort previewExpenseImportPort = mock(PreviewExpenseImportPort.class);
    private final ConfirmExpenseImportPort confirmExpenseImportPort = mock(ConfirmExpenseImportPort.class);
    private final GetExpenseImportBatchPort getExpenseImportBatchPort = mock(GetExpenseImportBatchPort.class);
    private final GenerateExpenseImportTemplatePort generateExpenseImportTemplatePort = mock(GenerateExpenseImportTemplatePort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpenseImportsController(previewExpenseImportPort, confirmExpenseImportPort, getExpenseImportBatchPort, generateExpenseImportTemplatePort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void templateReturnsExcelAttachment() throws Exception {
        when(generateExpenseImportTemplatePort.generate(1L)).thenReturn(new ExpenseImportTemplateResponse(
                "easy-finance-expense-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        ));

        mockMvc.perform(get("/api/v1/accounts/1/imports/expenses/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"easy-finance-expense-import-template.xlsx\""));

        verify(generateExpenseImportTemplatePort).generate(1L);
    }

    @Test
    void previewMultipartDelegates() throws Exception {
        when(previewExpenseImportPort.preview(any())).thenReturn(batch("PREVIEW"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "expenses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/expenses/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(77))
                .andExpect(jsonPath("$.rows[0].description").value("Lunch"));

        verify(previewExpenseImportPort).preview(any());
    }

    @Test
    void previewWithoutFileReturnsStableError() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/expenses/preview"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }

    @Test
    void previewInvalidExtensionReturnsStableError() throws Exception {
        when(previewExpenseImportPort.preview(any()))
                .thenThrow(new BusinessRuleViolationException("IMPORT_FILE_INVALID_TYPE", "Only .xlsx files are supported."));
        MockMultipartFile file = new MockMultipartFile("file", "expenses.xls", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/expenses/preview").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void confirmAndGetDelegate() throws Exception {
        when(confirmExpenseImportPort.confirm(1L, 77L)).thenReturn(batch("CONFIRMED"));
        when(getExpenseImportBatchPort.get(1L, 77L)).thenReturn(batch("PREVIEW"));

        mockMvc.perform(post("/api/v1/accounts/1/imports/expenses/77/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/accounts/1/imports/expenses/77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREVIEW"));
    }

    private static ExpenseImportBatchResponse batch(String status) {
        return new ExpenseImportBatchResponse(
                77L,
                1L,
                10L,
                "expenses.xlsx",
                status,
                1,
                1,
                0,
                "CONFIRMED".equals(status) ? Instant.parse("2026-05-12T10:15:30Z") : null,
                List.of(new ExpenseImportRowResponse(
                        101L,
                        2,
                        LocalDate.of(2026, 5, 1),
                        "Lunch",
                        new BigDecimal("120.00"),
                        "COP",
                        "Food",
                        20L,
                        "Cash",
                        30L,
                        "PAID",
                        true,
                        List.of(),
                        null
                ))
        );
    }
}
