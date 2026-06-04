package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.imports.application.port.in.GeneratePaymentMethodImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportPaymentMethodPort;
import com.easyfinance.imports.application.port.in.PreviewPaymentMethodImportPort;
import com.easyfinance.imports.application.response.PaymentMethodImportResponse;
import com.easyfinance.imports.application.response.PaymentMethodImportRowResponse;
import com.easyfinance.imports.application.response.PaymentMethodImportTemplateResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
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

class PaymentMethodImportsControllerTest {

    private final GeneratePaymentMethodImportTemplatePort generateTemplatePort = mock(GeneratePaymentMethodImportTemplatePort.class);
    private final ImportPaymentMethodPort importPort = mock(ImportPaymentMethodPort.class);
    private final PreviewPaymentMethodImportPort previewPort = mock(PreviewPaymentMethodImportPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentMethodImportsController(generateTemplatePort, importPort, previewPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void templateDownloadsWorkbook() throws Exception {
        when(generateTemplatePort.generate(1L)).thenReturn(
                new PaymentMethodImportTemplateResponse(
                        "easy-finance-payment-method-import-template.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[]{1, 2, 3}
                )
        );

        mockMvc.perform(get("/api/v1/accounts/1/imports/payment-methods/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void previewReturnsParsedRowData() throws Exception {
        when(previewPort.previewPaymentMethods(any())).thenReturn(new PaymentMethodImportResponse(
                0,
                List.of(row(null))
        ));
        MockMultipartFile file = file();

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/payment-methods/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.rows[0].name").value("Visa"))
                .andExpect(jsonPath("$.rows[0].description").value("Tarjeta principal"))
                .andExpect(jsonPath("$.rows[0].type").value("CREDIT_CARD"));
    }

    @Test
    void importDirectReturnsResponse() throws Exception {
        when(importPort.importPaymentMethods(any())).thenReturn(new PaymentMethodImportResponse(
                1,
                List.of(row(10L))
        ));
        MockMultipartFile file = file();

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/payment-methods").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].createdPaymentMethodId").value(10))
                .andExpect(jsonPath("$.rows[0].name").value("Visa"));
    }

    @Test
    void importDirectRequiresFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/payment-methods"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "payment-methods.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{1, 2, 3}
        );
    }

    private static PaymentMethodImportRowResponse row(Long createdId) {
        return new PaymentMethodImportRowResponse(2, "Visa", "Tarjeta principal", PaymentMethodType.CREDIT_CARD, true, createdId, List.of());
    }
}
