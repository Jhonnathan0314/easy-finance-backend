package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.imports.application.command.ImportPaymentMethodCommand;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApachePoiPaymentMethodImportParserTest {

    private final ApachePoiPaymentMethodImportParser parser = new ApachePoiPaymentMethodImportParser(1000);

    @Test
    void parsesValidRowsAndIgnoresBlankOnes() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Efectivo");
            row1.createCell(1).setCellValue("Efectivo");
            sheet.createRow(2);
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Banco");
            row3.createCell(1).setCellValue("BANK_ACCOUNT");
        });

        var rows = parser.parse(command(file));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).type()).isEqualTo(PaymentMethodType.CASH);
        assertThat(rows.get(1).type()).isEqualTo(PaymentMethodType.BANK_ACCOUNT);
    }

    @Test
    void invalidTypeAddsError() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("X");
            row.createCell(1).setCellValue("OtroTipo");
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
            row1.createCell(1).setCellValue("Efectivo");
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Banco");
            row2.createCell(1).setCellValue("");
        });

        var rows = parser.parse(command(file));
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().errors()).contains("Nombre requerido");
        assertThat(rows.get(1).errors()).contains("Tipo requerido");
    }

    @Test
    void detectsDuplicatesInsideFileByNormalizedName() throws Exception {
        byte[] file = workbookBytes(sheet -> {
            createHeader(sheet);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Nequi");
            row1.createCell(1).setCellValue("BilleteraDigital");
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(" nequi ");
            row2.createCell(1).setCellValue("OTHER");
        });

        var rows = parser.parse(command(file));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).errors()).contains("Medio de pago duplicado dentro del archivo");
    }

    private static void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Nombre");
        header.createCell(1).setCellValue("Tipo");
    }

    private static byte[] workbookBytes(java.util.function.Consumer<Sheet> populator) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("MediosPago");
            populator.accept(sheet);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static ImportPaymentMethodCommand command(byte[] bytes) {
        return new ImportPaymentMethodCommand(1L, "payment-methods.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.length, new ByteArrayInputStream(bytes));
    }
}

