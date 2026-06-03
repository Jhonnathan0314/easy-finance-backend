package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.validation.AnnualBudgetImportMonthScope;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApachePoiBudgetImportParserTest {

    private final ApachePoiBudgetImportParser parser = new ApachePoiBudgetImportParser(1000);

    @Test
    void parsesAllMonthsAndSpecificMonths() throws Exception {
        byte[] content = workbook(new String[]{"Año", "Mes", "NombrePresupuesto", "Categoria", "NombreSubpresupuesto", "Valor"},
                new Object[]{2026, "Todos", "Presupuesto 2026", "Mercado", "Mercado Casa", 800000},
                new Object[]{2026, "Marzo", "Presupuesto 2026", "Mercado", "Mercado Casa", 950000},
                new Object[]{2026, "7", "Presupuesto 2026", "Servicios", "Internet", 120000},
                new Object[]{2026, "", "Presupuesto 2026", "Ahorro", "Fondo", 50000}
        );

        var rows = parser.parse(command(content));

        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).monthScope()).isInstanceOf(AnnualBudgetImportMonthScope.AllMonths.class);
        assertThat(rows.get(1).monthScope()).isEqualTo(new AnnualBudgetImportMonthScope.SingleMonth(3));
        assertThat(rows.get(2).monthScope()).isEqualTo(new AnnualBudgetImportMonthScope.SingleMonth(7));
        assertThat(rows.get(3).monthScope()).isInstanceOf(AnnualBudgetImportMonthScope.AllMonths.class);
        assertThat(rows).allMatch(row -> row.errors().isEmpty());
    }

    @Test
    void validatesInvalidMonthAndYearAndRequiredFields() throws Exception {
        byte[] content = workbook(new String[]{"Año", "Mes", "NombrePresupuesto", "Categoria", "NombreSubpresupuesto", "Valor"},
                new Object[]{1999, "XMes", "Presupuesto", "", "", -10}
        );

        var rows = parser.parse(command(content));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.errors()).contains("Año fuera de rango", "Mes inválido", "Categoria requerido", "NombreSubpresupuesto requerido", "Valor debe ser mayor a 0");
        });
    }

    @Test
    void rejectsMissingHeaders() throws Exception {
        byte[] content = workbook(new String[]{"Año", "Mes"}, new Object[]{2026, "Todos"});

        assertThatThrownBy(() -> parser.parse(command(content)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("IMPORT_TEMPLATE_INVALID");
    }

    private static ImportAnnualBudgetCommand command(byte[] content) {
        return new ImportAnnualBudgetCommand(1L, "budgets.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content.length, new ByteArrayInputStream(content));
    }

    private static byte[] workbook(String[] headers, Object[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("PresupuestoAnual");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) {
                    Object value = rows[i][j];
                    if (value == null) {
                        continue;
                    }
                    var cell = row.createCell(j);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}

