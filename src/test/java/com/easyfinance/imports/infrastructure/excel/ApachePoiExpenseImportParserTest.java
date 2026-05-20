package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApachePoiExpenseImportParserTest {

    private final ApachePoiExpenseImportParser parser = new ApachePoiExpenseImportParser(2);

    @Test
    void parsesValidLegacyRowsAndIgnoresBlankRows() throws Exception {
        byte[] file = legacyWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID"},
                {null, null, null, null, null, null}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().valid()).isTrue();
        assertThat(rows.getFirst().amount().amount()).isEqualByComparingTo("120.50");
        assertThat(rows.getFirst().description()).isEqualTo("Lunch");
        assertThat(rows.getFirst().categoryName()).isEqualTo("Food");
        assertThat(rows.getFirst().appliesDebtPayment()).isFalse();
    }

    @Test
    void parsesDebtPaymentRowFromDynamicTemplate() throws Exception {
        byte[] file = debtWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID", "SI", "Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL", "INSTALLMENT", "Imported payment"}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isTrue();
            assertThat(row.appliesDebtPayment()).isTrue();
            assertThat(row.debtId()).isEqualTo(99L);
            assertThat(row.debtLabel()).isEqualTo("Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL");
            assertThat(row.debtPaymentType().name()).isEqualTo("INSTALLMENT");
            assertThat(row.debtPaymentNotes()).isEqualTo("Imported payment");
        });
    }

    @Test
    void emptyOrNoDebtPaymentFlagIsFalse() throws Exception {
        byte[] file = debtWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID", "", "", "", ""},
                {"2026-05-02", "Dinner", 50.00, "Food", "Cash", "PAID", "NO", "", "", ""}
        });

        var rows = new ApachePoiExpenseImportParser(10).parse(command(file), 1L);

        assertThat(rows).hasSize(2).allSatisfy(row -> {
            assertThat(row.valid()).isTrue();
            assertThat(row.appliesDebtPayment()).isFalse();
        });
    }

    @Test
    void invalidDebtPaymentTypeProducesRowError() throws Exception {
        byte[] file = debtWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID", "SI", "Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL", "OTHER", ""}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isFalse();
            assertThat(row.errors()).extracting("code").contains("IMPORT_DEBT_PAYMENT_TYPE_INVALID");
        });
    }

    @Test
    void debtFieldsWithNoFlagProduceRowError() throws Exception {
        byte[] file = debtWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID", "NO", "Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL", "", ""}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isFalse();
            assertThat(row.errors()).extracting("code").contains("IMPORT_DEBT_PAYMENT_FIELDS_NOT_ALLOWED");
        });
    }

    @Test
    void invalidDebtPaymentFlagProducesRowError() throws Exception {
        byte[] file = debtWorkbookWithRows(new Object[][]{
                {"2026-05-01", "Lunch", 120.50, "Food", "Cash", "PAID", "YES", "", "", ""}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isFalse();
            assertThat(row.errors()).extracting("code").contains("IMPORT_DEBT_PAYMENT_FLAG_INVALID");
        });
    }

    @Test
    void reportsInvalidRowValues() throws Exception {
        byte[] file = legacyWorkbookWithRows(new Object[][]{
                {"bad-date", "", -1, "Food", "Cash", "UNKNOWN"}
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isFalse();
            assertThat(row.errors()).extracting("code")
                    .contains("INVALID_DATE", "REQUIRED", "INVALID_AMOUNT", "INVALID_PAYMENT_STATE");
        });
    }

    @Test
    void parsesExcelDateCellAsSameLocalDate() throws Exception {
        byte[] file = workbookWithExcelDateCell(LocalDate.of(2026, 1, 31));

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row ->
                assertThat(row.expenseDate()).isEqualTo(LocalDate.of(2026, 1, 31)));
    }

    @Test
    void rejectsFormulaCells() throws Exception {
        byte[] file = workbookWithFormulaAmount();

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.valid()).isFalse();
            assertThat(row.errors()).extracting("code").contains("INVALID_AMOUNT");
        });
    }

    @Test
    void corruptFileFailsWithStableError() {
        assertThatThrownBy(() -> parser.parse(command(new byte[]{1, 2, 3}), 1L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        ex -> assertThat(ex.code()).isEqualTo("IMPORT_TEMPLATE_INVALID"));
    }

    @Test
    void missingHeadersFailTemplate() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("Gastos").createRow(0).createCell(0).setCellValue("Fecha");
            workbook.write(output);
            assertThatThrownBy(() -> parser.parse(command(output.toByteArray()), 1L))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("IMPORT_TEMPLATE_INVALID"));
        }
    }

    @Test
    void rowLimitFails() throws Exception {
        byte[] file = legacyWorkbookWithRows(new Object[][]{
                {"2026-05-01", "A", 1, "Food", "Cash", "PAID"},
                {"2026-05-02", "B", 1, "Food", "Cash", "PAID"},
                {"2026-05-03", "C", 1, "Food", "Cash", "PAID"}
        });

        assertThatThrownBy(() -> parser.parse(command(file), 1L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("IMPORT_ROW_LIMIT_EXCEEDED"));
    }

    private static PreviewExpenseImportCommand command(byte[] bytes) {
        return new PreviewExpenseImportCommand(1L, "expenses.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.length, new ByteArrayInputStream(bytes));
    }

    private static byte[] legacyWorkbookWithRows(Object[][] data) throws Exception {
        return workbookWithRows(new String[]{"Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago"}, data, false);
    }

    private static byte[] debtWorkbookWithRows(Object[][] data) throws Exception {
        return workbookWithRows(new String[]{"Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago", "AplicaPagoDeuda", "Deuda", "TipoPagoDeuda", "NotasPagoDeuda"}, data, true);
    }

    private static byte[] workbookWithRows(String[] headers, Object[][] data, boolean includeValuesSheet) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Gastos");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Object value = data[r][c];
                    if (value instanceof Number number) {
                        row.createCell(c).setCellValue(number.doubleValue());
                    } else if (value != null) {
                        row.createCell(c).setCellValue(value.toString());
                    }
                }
            }
            if (includeValuesSheet) {
                var values = workbook.createSheet("Valores");
                Row valuesHeader = values.createRow(0);
                valuesHeader.createCell(3).setCellValue("Deudas");
                valuesHeader.createCell(4).setCellValue("DeudaId");
                Row debt = values.createRow(1);
                debt.createCell(3).setCellValue("Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL");
                debt.createCell(4).setCellValue(99L);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] workbookWithExcelDateCell(LocalDate date) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Gastos");
            Row header = sheet.createRow(0);
            String[] headers = {"Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            CreationHelper helper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            Row row = sheet.createRow(1);
            var dateCell = row.createCell(0);
            dateCell.setCellValue(LocalDateTime.of(date, java.time.LocalTime.NOON));
            dateCell.setCellStyle(dateStyle);
            row.createCell(1).setCellValue("Lunch");
            row.createCell(2).setCellValue(120.50);
            row.createCell(3).setCellValue("Food");
            row.createCell(4).setCellValue("Cash");
            row.createCell(5).setCellValue("PAID");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] workbookWithFormulaAmount() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Gastos");
            Row header = sheet.createRow(0);
            String[] headers = {"Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-05-01");
            row.createCell(1).setCellValue("Lunch");
            row.createCell(2).setCellFormula("1+1");
            row.createCell(3).setCellValue("Food");
            row.createCell(4).setCellValue("Cash");
            row.createCell(5).setCellValue("PAID");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
