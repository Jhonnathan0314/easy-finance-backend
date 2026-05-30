package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.port.in.GenerateCategoryImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportCategoryPort;
import com.easyfinance.imports.application.response.CategoryImportResponse;
import com.easyfinance.imports.application.response.CategoryImportRowResponse;
import com.easyfinance.imports.application.response.CategoryImportTemplateResponse;
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

class CategoryImportsControllerTest {

    private final GenerateCategoryImportTemplatePort generateTemplatePort = mock(GenerateCategoryImportTemplatePort.class);
    private final ImportCategoryPort importCategoryPort = mock(ImportCategoryPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CategoryImportsController(generateTemplatePort, importCategoryPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void templateDownloadsWorkbook() throws Exception {
        when(generateTemplatePort.generate(1L)).thenReturn(
                new CategoryImportTemplateResponse(
                        "easy-finance-category-import-template.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        new byte[]{1, 2, 3}
                )
        );

        mockMvc.perform(get("/api/v1/accounts/1/imports/categories/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void importDirectReturnsResponse() throws Exception {
        when(importCategoryPort.importCategories(any())).thenReturn(new CategoryImportResponse(
                1,
                List.of(new CategoryImportRowResponse(2, true, 10L, List.of()))
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "categories.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/categories").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].createdCategoryId").value(10));
    }

    @Test
    void importDirectRequiresFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/categories"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }
}

