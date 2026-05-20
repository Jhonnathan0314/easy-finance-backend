package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.template.ExpenseImportTemplateData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePoiExpenseImportTemplateGeneratorTest {

    private final ApachePoiExpenseImportTemplateGenerator generator = new ApachePoiExpenseImportTemplateGenerator();

    @Test
    void generatedWorkbookHasExpectedSheetsHeadersAndValues() throws Exception {
        byte[] bytes = generator.generate(new ExpenseImportTemplateData(
                List.of("Alimentación", "Transporte"),
                List.of("Efectivo", "Visa")
        ));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet gastos = workbook.getSheet("Gastos");
            Sheet valores = workbook.getSheet("Valores");

            assertThat(gastos).isNotNull();
            assertThat(valores).isNotNull();
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(valores))).isTrue();
            assertThat(headerValues(gastos.getRow(0))).containsExactly("Fecha", "Descripción", "Monto", "Categoría", "MedioPago", "EstadoPago");
            assertThat(valores.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Alimentación");
            assertThat(valores.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Transporte");
            assertThat(valores.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Efectivo");
            assertThat(valores.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Visa");
            assertThat(valores.getRow(1).getCell(2).getStringCellValue()).isEqualTo("PENDING");
            assertThat(valores.getRow(2).getCell(2).getStringCellValue()).isEqualTo("PARTIAL");
            assertThat(valores.getRow(3).getCell(2).getStringCellValue()).isEqualTo("PAID");
            assertThat(gastos.getDataValidations()).hasSizeGreaterThanOrEqualTo(5);
        }
    }

    @Test
    void generatedWorkbookCanBeFilledAndParsedByCurrentParser() throws Exception {
        byte[] template = generator.generate(new ExpenseImportTemplateData(List.of("Food"), List.of("Cash")));
        byte[] filled = fillValidExpenseRow(template);

        var rows = new ApachePoiExpenseImportParser(1000).parse(command(filled), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isTrue();
            assertThat(row.description()).isEqualTo("Lunch");
            assertThat(row.categoryName()).isEqualTo("Food");
            assertThat(row.paymentMethodName()).isEqualTo("Cash");
            assertThat(row.paymentState().name()).isEqualTo("PAID");
        });
    }

    @Test
    void generatedWorkbookSupportsEmptyCatalogValues() throws Exception {
        byte[] bytes = generator.generate(new ExpenseImportTemplateData(List.of(), List.of()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet valores = workbook.getSheet("Valores");

            assertThat(valores).isNotNull();
            assertThat(valores.getRow(1).getCell(2).getStringCellValue()).isEqualTo("PENDING");
        }
    }

    private static List<String> headerValues(Row row) {
        return List.of(
                row.getCell(0).getStringCellValue(),
                row.getCell(1).getStringCellValue(),
                row.getCell(2).getStringCellValue(),
                row.getCell(3).getStringCellValue(),
                row.getCell(4).getStringCellValue(),
                row.getCell(5).getStringCellValue()
        );
    }

    private static byte[] fillValidExpenseRow(byte[] template) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet gastos = workbook.getSheet("Gastos");
            Row row = gastos.getRow(1);
            row.createCell(0).setCellValue("2026-05-01");
            row.createCell(1).setCellValue("Lunch");
            row.createCell(2).setCellValue(120.50);
            row.createCell(3).setCellValue("Food");
            row.createCell(4).setCellValue("Cash");
            row.createCell(5).setCellValue("PAID");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static PreviewExpenseImportCommand command(byte[] bytes) {
        return new PreviewExpenseImportCommand(1L, "expenses.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.length, new ByteArrayInputStream(bytes));
    }
}
