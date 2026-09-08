package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.port.out.DebtImportTemplateGeneratorPort;
import com.easyfinance.imports.application.template.DebtImportTemplateData;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ApachePoiDebtImportTemplateGenerator implements DebtImportTemplateGeneratorPort {

    private static final int MAX_DATA_ROWS = 1000;
    private static final String DEBTS_SHEET = "Deudas";
    private static final String VALUES_SHEET = "Valores";
    private static final String PARTICIPANT_RANGE = "ParticipantesDeuda";
    private static final String[] HEADERS = {
            "Nombre", "Descripcion", "Capital", "SaldoPendiente", "NumeroCuotas",
            "ValorCuota", "FechaInicio", "FechaVencimiento", "Participante", "Notas", "Estado"
    };

    @Override
    public byte[] generate(DebtImportTemplateData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet debts = workbook.createSheet(DEBTS_SHEET);
            Sheet values = workbook.createSheet(VALUES_SHEET);
            CreationHelper creationHelper = workbook.getCreationHelper();

            createValuesSheet(values, data);
            createNamedRange(workbook, data.participantLabels().size());
            createDebtsSheet(workbook, debts, creationHelper);
            workbook.setSheetHidden(workbook.getSheetIndex(values), true);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Debt import template could not be generated.", ex);
        }
    }

    private static void createValuesSheet(Sheet sheet, DebtImportTemplateData data) {
        List<String> participantLabels = data.participantLabels();
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Participantes");
        header.createCell(1).setCellValue("Instrucciones");
        header.createCell(2).setCellValue(
                "No modificar cabeceras. Maximo 1000 filas. NumeroCuotas y ValorCuota deben completarse juntos o "
                        + "ambos vacios. SaldoPendiente es opcional y representa cuanto queda pendiente hoy (util al "
                        + "migrar una deuda que ya se viene pagando); si se deja vacio, la deuda inicia con el capital "
                        + "completo pendiente. Participante es opcional; vacio usa el usuario logueado."
        );

        int rows = Math.max(participantLabels.size(), 1);
        for (int i = 0; i < rows; i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) {
                row = sheet.createRow(i + 1);
            }
            if (i < participantLabels.size()) {
                row.createCell(0).setCellValue(participantLabels.get(i));
            }
        }
        if (participantLabels.isEmpty()) {
            sheet.getRow(1).createCell(1).setCellValue("No hay participantes activos para esta cuenta.");
        }

        sheet.setColumnWidth(0, 42 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 90 * 256);
    }

    private static void createNamedRange(Workbook workbook, int participantCount) {
        org.apache.poi.ss.usermodel.Name range = workbook.createName();
        range.setNameName(PARTICIPANT_RANGE);
        int lastRow = Math.max(participantCount, 1) + 1;
        range.setRefersToFormula("'" + VALUES_SHEET + "'!$A$2:$A$" + lastRow);
    }

    private static void createDebtsSheet(Workbook workbook, Sheet sheet, CreationHelper creationHelper) {
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0.00"));

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int rowIndex = 1; rowIndex <= MAX_DATA_ROWS; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.createCell(2).setCellStyle(amountStyle);
            row.createCell(3).setCellStyle(amountStyle);
            row.createCell(5).setCellStyle(amountStyle);
            row.createCell(6).setCellStyle(dateStyle);
            row.createCell(7).setCellStyle(dateStyle);
        }

        addDateValidation(sheet, 6);
        addDateValidation(sheet, 7);
        addAmountValidation(sheet, 2, "Capital invalido", "Use un numero mayor a 0.");
        addAmountValidation(sheet, 3, "SaldoPendiente invalido", "Use un numero mayor a 0 o deje vacio.");
        addAmountValidation(sheet, 5, "ValorCuota invalido", "Use un numero mayor a 0 o deje vacio.");
        addFormulaListValidation(sheet, 8, PARTICIPANT_RANGE, "Participante invalido", "Use un participante activo de la hoja Valores o deje vacio.");

        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 36 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 14 * 256);
        sheet.setColumnWidth(7, 16 * 256);
        sheet.setColumnWidth(8, 42 * 256);
        sheet.setColumnWidth(9, 36 * 256);
    }

    private static void addDateValidation(Sheet sheet, int columnIndex) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createDateConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "DATE(2000,1,1)",
                "DATE(2100,12,31)",
                "yyyy-mm-dd"
        );
        DataValidation validation = helper.createValidation(constraint, rows(columnIndex));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Fecha invalida", "Use formato yyyy-mm-dd.");
        sheet.addValidationData(validation);
    }

    private static void addAmountValidation(Sheet sheet, int columnIndex, String title, String message) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createDecimalConstraint(
                DataValidationConstraint.OperatorType.GREATER_THAN,
                "0",
                null
        );
        DataValidation validation = helper.createValidation(constraint, rows(columnIndex));
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
    }

    private static void addFormulaListValidation(Sheet sheet, int columnIndex, String rangeName, String title, String message) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        DataValidation validation = helper.createValidation(constraint, rows(columnIndex));
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
    }

    private static CellRangeAddressList rows(int columnIndex) {
        return new CellRangeAddressList(1, MAX_DATA_ROWS, columnIndex, columnIndex);
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
