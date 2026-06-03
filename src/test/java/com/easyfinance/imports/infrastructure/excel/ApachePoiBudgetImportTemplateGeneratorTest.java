package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.template.AnnualBudgetImportTemplateData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiBudgetImportTemplateGeneratorTest {

    private final ApachePoiBudgetImportTemplateGenerator generator = new ApachePoiBudgetImportTemplateGenerator();

    @Test
    void generatesTemplateWithExpectedHeadersAndValues() throws Exception {
        byte[] content = generator.generate(new AnnualBudgetImportTemplateData(List.of("Mercado", "Servicios")));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var budgetSheet = workbook.getSheet("PresupuestoAnual");
            assertThat(budgetSheet).isNotNull();
            assertThat(budgetSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Año");
            assertThat(budgetSheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Mes");
            assertThat(budgetSheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("NombrePresupuesto");
            assertThat(budgetSheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Categoria");
            assertThat(budgetSheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("NombreSubpresupuesto");
            assertThat(budgetSheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("Valor");

            var valuesSheet = workbook.getSheet("Valores");
            assertThat(valuesSheet).isNotNull();
            assertThat(valuesSheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Todos");
            assertThat(valuesSheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Enero");
            assertThat(valuesSheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Mercado");
            assertThat(valuesSheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Servicios");
        }
    }
}

