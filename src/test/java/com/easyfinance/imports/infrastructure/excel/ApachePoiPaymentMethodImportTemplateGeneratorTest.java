package com.easyfinance.imports.infrastructure.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiPaymentMethodImportTemplateGeneratorTest {

    private final ApachePoiPaymentMethodImportTemplateGenerator generator = new ApachePoiPaymentMethodImportTemplateGenerator();

    @Test
    void generatesTemplateWithExpectedSheetsAndHeaders() throws Exception {
        byte[] bytes = generator.generate();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet main = workbook.getSheet("MediosPago");
            assertThat(main).isNotNull();
            assertThat(main.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Nombre");
            assertThat(main.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Tipo");

            Sheet values = workbook.getSheet("Valores");
            assertThat(values).isNotNull();
            assertThat(values.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Efectivo");
            assertThat(values.getRow(2).getCell(0).getStringCellValue()).isEqualTo("CuentaBancaria");
            assertThat(values.getRow(3).getCell(0).getStringCellValue()).isEqualTo("TarjetaCredito");
            assertThat(values.getRow(4).getCell(0).getStringCellValue()).isEqualTo("TarjetaDebito");
            assertThat(values.getRow(5).getCell(0).getStringCellValue()).isEqualTo("BilleteraDigital");
            assertThat(values.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Otro");
        }
    }
}

