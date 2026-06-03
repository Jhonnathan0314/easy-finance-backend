package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportIncomeCommand;
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

class ApachePoiIncomeImportParserTest {

    private final ApachePoiIncomeImportParser parser = new ApachePoiIncomeImportParser(1000);

    @Test
    void parsesValidRowsAndIgnoresBlankRows() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Ingresos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Descripcion");
            header.createCell(2).setCellValue("Categoria");
            header.createCell(3).setCellValue("Monto");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-05-10");
            row.createCell(1).setCellValue("Nomina");
            row.createCell(2).setCellValue("Salario");
            row.createCell(3).setCellValue(5200000);
            sheet.createRow(2);
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().incomeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(rows.getFirst().amount()).isEqualByComparingTo(new BigDecimal("5200000.0"));
        assertThat(rows.getFirst().errors()).isEmpty();
    }

    @Test
    void parsesTemplateWithDateHeaderFormatSuffix() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Ingresos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha (yyyy-MM-dd)");
            header.createCell(1).setCellValue("Descripcion");
            header.createCell(2).setCellValue("Categoria");
            header.createCell(3).setCellValue("Monto");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026-05-11");
            row.createCell(1).setCellValue("Freelance");
            row.createCell(2).setCellValue("Servicios");
            row.createCell(3).setCellValue(1000000);
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().incomeDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(rows.getFirst().errors()).isEmpty();
    }

    @Test
    void reportsValidationErrors() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Ingresos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Descripcion");
            header.createCell(2).setCellValue("Categoria");
            header.createCell(3).setCellValue("Monto");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("bad-date");
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue(-10);
        });

        var rows = parser.parse(command(file), 1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().valid()).isFalse();
        assertThat(rows.getFirst().errors()).contains("Fecha invalida", "Descripcion requerida", "Categoria requerida", "Monto debe ser mayor a 0");
    }

    @Test
    void invalidHeaderFails() throws Exception {
        byte[] file = workbookBytes(workbook -> {
            var sheet = workbook.createSheet("Ingresos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("X");
        });

        assertThatThrownBy(() -> parser.parse(command(file), 1L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_TEMPLATE_INVALID"));
    }

    private static ImportIncomeCommand command(byte[] content) {
        return new ImportIncomeCommand(1L, "incomes.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content.length, new ByteArrayInputStream(content));
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
