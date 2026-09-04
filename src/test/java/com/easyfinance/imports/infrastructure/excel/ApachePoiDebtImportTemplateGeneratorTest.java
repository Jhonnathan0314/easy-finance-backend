package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.template.DebtImportTemplateData;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiDebtImportTemplateGeneratorTest {

    private final ApachePoiDebtImportTemplateGenerator generator = new ApachePoiDebtImportTemplateGenerator();

    @Test
    void generatesDebtTemplateWithExpectedHeadersAndValidation() throws Exception {
        byte[] content = generator.generate(new DebtImportTemplateData(
                List.of("Usuario Actual <user@example.com>", "Ana Gomez <ana@example.com>")
        ));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var debts = workbook.getSheet("Deudas");
            var values = workbook.getSheet("Valores");
            assertThat(debts).isNotNull();
            assertThat(values).isNotNull();
            assertThat(debts.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nombre");
            assertThat(debts.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Descripcion");
            assertThat(debts.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Capital");
            assertThat(debts.getRow(0).getCell(3).getStringCellValue()).isEqualTo("SaldoPendiente");
            assertThat(debts.getRow(0).getCell(4).getStringCellValue()).isEqualTo("NumeroCuotas");
            assertThat(debts.getRow(0).getCell(5).getStringCellValue()).isEqualTo("ValorCuota");
            assertThat(debts.getRow(0).getCell(6).getStringCellValue()).isEqualTo("FechaInicio");
            assertThat(debts.getRow(0).getCell(7).getStringCellValue()).isEqualTo("FechaVencimiento");
            assertThat(debts.getRow(0).getCell(8).getStringCellValue()).isEqualTo("Participante");
            assertThat(debts.getRow(0).getCell(9).getStringCellValue()).isEqualTo("Notas");
            assertThat(values.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Usuario Actual <user@example.com>");
            assertThat(values.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Ana Gomez <ana@example.com>");
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(values))).isTrue();

            List<? extends DataValidation> validations = debts.getDataValidations();
            assertThat(validations).isNotEmpty();
        }
    }
}
