package com.easyfinance.imports.infrastructure.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiCategoryImportTemplateGeneratorTest {

    private final ApachePoiCategoryImportTemplateGenerator generator = new ApachePoiCategoryImportTemplateGenerator();

    @Test
    void generatesTemplateWithExpectedSheetsAndHeaders() throws Exception {
        byte[] bytes = generator.generate();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet categories = workbook.getSheet("Categorias");
            assertThat(categories).isNotNull();
            assertThat(categories.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nombre");
            assertThat(categories.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Tipo");
            assertThat(categories.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Descripcion");

            Sheet values = workbook.getSheet("Valores");
            assertThat(values).isNotNull();
            assertThat(values.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Gasto");
            assertThat(values.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Ingreso");
        }
    }
}
