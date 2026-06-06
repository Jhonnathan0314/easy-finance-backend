package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.template.IncomeImportTemplateData;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiIncomeImportTemplateGeneratorTest {

    private final ApachePoiIncomeImportTemplateGenerator generator = new ApachePoiIncomeImportTemplateGenerator();

    @Test
    void generatesIncomeTemplateWithExpectedHeadersAndValidation() throws Exception {
        byte[] content = generator.generate(new IncomeImportTemplateData(
                List.of("Salario", "Freelance"),
                List.of("Usuario Actual <user@example.com>", "Ana Gomez <ana@example.com>")
        ));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var incomes = workbook.getSheet("Ingresos");
            var values = workbook.getSheet("Valores");
            assertThat(incomes).isNotNull();
            assertThat(values).isNotNull();
            assertThat(incomes.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Fecha (yyyy-MM-dd)");
            assertThat(incomes.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Descripcion");
            assertThat(incomes.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Categoria");
            assertThat(incomes.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Monto");
            assertThat(incomes.getRow(0).getCell(4).getStringCellValue()).isEqualTo("Participante");
            assertThat(values.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Salario");
            assertThat(values.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Freelance");
            assertThat(values.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Usuario Actual <user@example.com>");
            assertThat(values.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Ana Gomez <ana@example.com>");
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(values))).isTrue();

            List<? extends DataValidation> validations = incomes.getDataValidations();
            assertThat(validations).isNotEmpty();
        }
    }
}
