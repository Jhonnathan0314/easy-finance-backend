package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.port.in.GenerateCategoryImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportCategoryPort;
import com.easyfinance.imports.application.port.in.PreviewCategoryImportPort;
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
    private final PreviewCategoryImportPort previewCategoryImportPort = mock(PreviewCategoryImportPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CategoryImportsController(generateTemplatePort, importCategoryPort, previewCategoryImportPort))
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
    void previewReturnsParsedRowData() throws Exception {
        when(previewCategoryImportPort.previewCategories(any())).thenReturn(new CategoryImportResponse(
                0,
                List.of(row(null))
        ));
        MockMultipartFile file = file();

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/categories/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.rows[0].name").value("Mercado"))
                .andExpect(jsonPath("$.rows[0].description").value("Compras de mercado"))
                .andExpect(jsonPath("$.rows[0].type").value("EXPENSE"));
    }

    @Test
    void importDirectReturnsResponse() throws Exception {
        when(importCategoryPort.importCategories(any())).thenReturn(new CategoryImportResponse(
                1,
                List.of(row(10L))
        ));
        MockMultipartFile file = file();

        mockMvc.perform(multipart("/api/v1/accounts/1/imports/categories").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].createdCategoryId").value(10))
                .andExpect(jsonPath("$.rows[0].name").value("Mercado"));
    }

    @Test
    void importDirectRequiresFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/accounts/1/imports/categories"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_REQUIRED"));
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "categories.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[]{1, 2, 3}
        );
    }

    private static CategoryImportRowResponse row(Long createdId) {
        return new CategoryImportRowResponse(2, "Mercado", "Compras de mercado", CategoryType.EXPENSE, true, createdId, List.of());
    }
}
