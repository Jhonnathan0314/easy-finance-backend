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
                List.of("AlimentaciÃƒÂ³n", "Transporte"),
                List.of("Efectivo", "Visa"),
                List.of(new ExpenseImportTemplateData.DebtOption(99L, "Loan | Saldo: 120000.00 | Inicio: 2026-05-01 | MANUAL")), 
                List.of("Ana Finance <ana@example.com>")
        ));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet gastos = workbook.getSheet("Gastos");
            Sheet valores = workbook.getSheet("Valores");

            assertThat(gastos).isNotNull();
            assertThat(valores).isNotNull();
            assertThat(workbook.isSheetHidden(workbook.getSheetIndex(valores))).isTrue();
            assertThat(headerValues(gastos.getRow(0))).containsExactly("Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago", "AplicaPagoDeuda", "Deuda", "TipoPagoDeuda", "NotasPagoDeuda", "Participante");
            assertThat(valores.getRow(1).getCell(0).getStringCellValue()).isEqualTo("AlimentaciÃƒÂ³n");
            assertThat(valores.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Transporte");
            assertThat(valores.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Efectivo");
            assertThat(valores.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Visa");
            assertThat(valores.getRow(1).getCell(2).getStringCellValue()).isEqualTo("PENDING");
            assertThat(valores.getRow(2).getCell(2).getStringCellValue()).isEqualTo("PARTIAL");
            assertThat(valores.getRow(3).getCell(2).getStringCellValue()).isEqualTo("PAID");
            assertThat(valores.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Loan | Saldo: 120000.00 | Inicio: 2026-05-01 | MANUAL");
            assertThat(valores.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(99D);
            assertThat(valores.getRow(1).getCell(5).getStringCellValue()).isEqualTo("SI");
            assertThat(valores.getRow(2).getCell(5).getStringCellValue()).isEqualTo("NO");
            assertThat(valores.getRow(1).getCell(6).getStringCellValue()).isEqualTo("INSTALLMENT");
            assertThat(valores.getRow(2).getCell(6).getStringCellValue()).isEqualTo("CAPITAL_PAYMENT");
            assertThat(valores.getRow(1).getCell(7).getStringCellValue()).isEqualTo("Ana Finance <ana@example.com>");
            assertThat(valores.getRow(0).getCell(9).getStringCellValue()).contains("1500 filas");
            assertThat(gastos.getRow(1500)).isNotNull();
            assertThat(gastos.getRow(1501)).isNull();
            assertThat(gastos.getDataValidations()).hasSizeGreaterThanOrEqualTo(9);
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
            assertThat(row.appliesDebtPayment()).isFalse();
        });
    }

    @Test
    void generatedWorkbookSupportsEmptyCatalogValues() throws Exception {
        byte[] bytes = generator.generate(new ExpenseImportTemplateData(List.of(), List.of()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet valores = workbook.getSheet("Valores");

            assertThat(valores).isNotNull();
            assertThat(valores.getRow(1).getCell(2).getStringCellValue()).isEqualTo("PENDING");
            assertThat(valores.getRow(1).getCell(5).getStringCellValue()).isEqualTo("SI");
            assertThat(valores.getRow(1).getCell(6).getStringCellValue()).isEqualTo("INSTALLMENT");
        }
    }

    private static List<String> headerValues(Row row) {
        return List.of(
                row.getCell(0).getStringCellValue(),
                row.getCell(1).getStringCellValue(),
                row.getCell(2).getStringCellValue(),
                row.getCell(3).getStringCellValue(),
                row.getCell(4).getStringCellValue(),
                row.getCell(5).getStringCellValue(),
                row.getCell(6).getStringCellValue(),
                row.getCell(7).getStringCellValue(),
                row.getCell(8).getStringCellValue(),
                row.getCell(9).getStringCellValue(),
                row.getCell(10).getStringCellValue()
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
