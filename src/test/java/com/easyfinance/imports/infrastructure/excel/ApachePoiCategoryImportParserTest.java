package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApachePoiCategoryImportParserTest {

    private final ApachePoiCategoryImportParser parser = new ApachePoiCategoryImportParser(1000);

    @Test
    void parsesValidRowsAndIgnoresBlankOnes() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Mercado");
            row1.createCell(1).setCellValue("Gasto");
            sheet.createRow(2); // blank row
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Nomina");
            row3.createCell(1).setCellValue("INCOME");
        });

        var rows = parser.parse(command(file));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(rows.get(1).type()).isEqualTo(CategoryType.INCOME);
        assertThat(rows.get(0).description()).isNull();
    }

    @Test
    void invalidTypeAddsError() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("X");
            row.createCell(1).setCellValue("Otro");
        });

        var rows = parser.parse(command(file));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().errors()).contains("Tipo invalido");
    }

    @Test
    void missingRequiredHeadersFails() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
        });

        assertThatThrownBy(() -> parser.parse(command(file)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_TEMPLATE_INVALID"));
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("");
            row1.createCell(1).setCellValue("Gasto");
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Mercado");
            row2.createCell(1).setCellValue("");
        });

        var rows = parser.parse(command(file));
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().errors()).contains("Nombre requerido");
        assertThat(rows.get(1).errors()).contains("Tipo requerido");
    }

    @Test
    void detectsDuplicatesInsideFile() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Mercado");
            row1.createCell(1).setCellValue("Gasto");
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(" mercado ");
            row2.createCell(1).setCellValue("EXPENSE");
        });

        var rows = parser.parse(command(file));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).errors()).contains("Categoria duplicada dentro del archivo");
    }

    @Test
    void parsesOptionalDescriptionWhenPresentAndTrimsIt() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Tipo");
            header.createCell(2).setCellValue("Descripcion");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Mercado");
            row.createCell(1).setCellValue("Gasto");
            row.createCell(2).setCellValue("  Compras del hogar  ");
        });

        var rows = parser.parse(command(file));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().description()).isEqualTo("Compras del hogar");
    }

    private static void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Tipo");
    }

    private static byte[] workbookBytes(java.util.function.Consumer<Sheet> populator) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Categorias");
            populator.accept(sheet);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static ImportCategoryCommand command(byte[] bytes) {
        return new ImportCategoryCommand(1L, "categories.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.length, new ByteArrayInputStream(bytes));
    }
}
