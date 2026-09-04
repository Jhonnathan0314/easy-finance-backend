package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApachePoiDebtImportParserTest {

    private final ApachePoiDebtImportParser parser = new ApachePoiDebtImportParser(1000);

    @Test
    void parsesValidRowsAndIgnoresBlankRows() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            header(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Prestamo carro");
            row.createCell(2).setCellValue(5000000);
            row.createCell(6).setCellValue("2026-05-01");
            sheet.createRow(2);
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().name()).isEqualTo("Prestamo carro");
        assertThat(rows.getFirst().totalAmount()).isEqualByComparingTo(new BigDecimal("5000000.0"));
        assertThat(rows.getFirst().startDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(rows.getFirst().errors()).isEmpty();
    }

    @Test
    void parsesOptionalColumnsIncludingRemainingBalanceAndInstallments() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            header(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Prestamo carro");
            row.createCell(1).setCellValue("Compra de vehiculo");
            row.createCell(2).setCellValue(5000000);
            row.createCell(3).setCellValue(3000000);
            row.createCell(4).setCellValue(12);
            row.createCell(5).setCellValue(450000);
            row.createCell(6).setCellValue("2026-05-01");
            row.createCell(7).setCellValue("2027-05-01");
            row.createCell(8).setCellValue("Ana Gomez <ana@example.com>");
            row.createCell(9).setCellValue("Deuda migrada de otra herramienta");
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        var row = rows.getFirst();
        assertThat(row.description()).isEqualTo("Compra de vehiculo");
        assertThat(row.remainingBalance()).isEqualByComparingTo(new BigDecimal("3000000.0"));
        assertThat(row.installmentCount()).isEqualTo(12);
        assertThat(row.installmentAmount()).isEqualByComparingTo(new BigDecimal("450000.0"));
        assertThat(row.dueDate()).isEqualTo(LocalDate.of(2027, 5, 1));
        assertThat(row.participantLabel()).isEqualTo("Ana Gomez <ana@example.com>");
        assertThat(row.notes()).isEqualTo("Deuda migrada de otra herramienta");
        assertThat(row.errors()).isEmpty();
    }

    @Test
    void reportsInstallmentPairRequiredWhenOnlyOneOfCountOrAmountIsPresent() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            header(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Prestamo carro");
            row.createCell(2).setCellValue(5000000);
            row.createCell(4).setCellValue(12);
            row.createCell(6).setCellValue("2026-05-01");
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().valid()).isFalse();
        assertThat(rows.getFirst().errors()).contains("Numero de cuotas y valor de cuota deben completarse juntos");
    }

    @Test
    void reportsRemainingBalanceGreaterThanCapital() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            header(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Prestamo carro");
            row.createCell(2).setCellValue(1000000);
            row.createCell(3).setCellValue(1500000);
            row.createCell(6).setCellValue("2026-05-01");
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().valid()).isFalse();
        assertThat(rows.getFirst().errors()).contains("Saldo pendiente no puede superar el capital");
    }

    @Test
    void reportsValidationErrors() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            header(sheet);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("");
            row.createCell(2).setCellValue(-10);
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().valid()).isFalse();
        assertThat(rows.getFirst().errors()).contains("Nombre requerido", "Capital debe ser mayor a 0", "FechaInicio requerida");
    }

    @Test
    void invalidHeaderFails() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Deudas");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("X");
        });

        assertThatThrownBy(() -> parser.parse(command(file), 1L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_TEMPLATE_INVALID"));
    }

    private static void header(org.apache.poi.ss.usermodel.Sheet sheet) {
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Descripcion");
        header.createCell(2).setCellValue("Capital");
        header.createCell(3).setCellValue("SaldoPendiente");
        header.createCell(4).setCellValue("NumeroCuotas");
        header.createCell(5).setCellValue("ValorCuota");
        header.createCell(6).setCellValue("FechaInicio");
        header.createCell(7).setCellValue("FechaVencimiento");
        header.createCell(8).setCellValue("Participante");
        header.createCell(9).setCellValue("Notas");
    }

    private static ImportDebtCommand command(byte[] content) {
        return new ImportDebtCommand(1L, "debts.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content.length, new ByteArrayInputStream(content));
    }

    private static byte[] workbookBytes(WorkbookConfigurer configurer) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurer.configure(workbook);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface WorkbookConfigurer {
        void configure(Workbook workbook) throws Exception;
    }
}
