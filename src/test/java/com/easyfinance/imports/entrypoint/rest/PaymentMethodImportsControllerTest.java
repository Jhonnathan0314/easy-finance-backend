package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GeneratePaymentMethodImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportPaymentMethodPort;
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
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentMethodImportsController(generateTemplatePort, importPort))
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
    void importDirectReturnsResponse() throws Exception {
        when(importPort.importPaymentMethods(any())).thenReturn(new PaymentMethodImportResponse(
                1,
                List.of(new PaymentMethodImportRowResponse(2, true, 10L, List.of()))
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payment-methods.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/payment-methods").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].createdPaymentMethodId").value(10));
    }

    @Test
    void importDirectRequiresFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/payment-methods"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }
}

