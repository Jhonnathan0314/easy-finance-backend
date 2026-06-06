package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.port.out.IncomeImportTemplateGeneratorPort;
import com.easyfinance.imports.application.template.IncomeImportTemplateData;
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
public class ApachePoiIncomeImportTemplateGenerator implements IncomeImportTemplateGeneratorPort {

    private static final int MAX_DATA_ROWS = 1000;
    private static final String INCOMES_SHEET = "Ingresos";
    private static final String VALUES_SHEET = "Valores";
    private static final String CATEGORY_RANGE = "CategoriasIngreso";
    private static final String PARTICIPANT_RANGE = "ParticipantesIngreso";
    private static final String[] HEADERS = {"Fecha (yyyy-MM-dd)", "Descripcion", "Categoria", "Monto", "Participante"};

    @Override
    public byte[] generate(IncomeImportTemplateData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet incomes = workbook.createSheet(INCOMES_SHEET);
            Sheet values = workbook.createSheet(VALUES_SHEET);
            CreationHelper creationHelper = workbook.getCreationHelper();

            createValuesSheet(values, data);
            createNamedRanges(workbook, data.categoryNames().size(), data.participantLabels().size());
            createIncomesSheet(workbook, incomes, creationHelper);
            workbook.setSheetHidden(workbook.getSheetIndex(values), true);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Income import template could not be generated.", ex);
        }
    }

    private static void createValuesSheet(Sheet sheet, IncomeImportTemplateData data) {
        List<String> categoryNames = data.categoryNames();
        List<String> participantLabels = data.participantLabels();
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Categorias");
        header.createCell(1).setCellValue("Participantes");
        header.createCell(2).setCellValue("Instrucciones");
        header.createCell(3).setCellValue("No modificar cabeceras. Maximo 1000 filas. Categoria debe existir activa y ser INCOME. Participante es opcional; vacio usa el usuario logueado.");

        int rows = Math.max(Math.max(categoryNames.size(), participantLabels.size()), 1);
        for (int i = 0; i < rows; i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) {
                row = sheet.createRow(i + 1);
            }
            if (i < categoryNames.size()) {
                row.createCell(0).setCellValue(categoryNames.get(i));
            }
            if (i < participantLabels.size()) {
                row.createCell(1).setCellValue(participantLabels.get(i));
            }
        }
        if (categoryNames.isEmpty()) {
            sheet.getRow(1).createCell(2).setCellValue("No hay categorias INCOME activas para esta cuenta.");
        }
        if (participantLabels.isEmpty()) {
            sheet.getRow(1).createCell(3).setCellValue("No hay participantes activos para esta cuenta.");
        }

        sheet.setColumnWidth(0, 36 * 256);
        sheet.setColumnWidth(1, 42 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 90 * 256);
    }

    private static void createNamedRanges(Workbook workbook, int categoryCount, int participantCount) {
        createNamedRange(workbook, CATEGORY_RANGE, 0, Math.max(categoryCount, 1));
        createNamedRange(workbook, PARTICIPANT_RANGE, 1, Math.max(participantCount, 1));
    }

    private static void createNamedRange(Workbook workbook, String name, int columnIndex, int valueCount) {
        org.apache.poi.ss.usermodel.Name range = workbook.createName();
        range.setNameName(name);
        char column = (char) ('A' + columnIndex);
        int lastRow = valueCount + 1;
        range.setRefersToFormula("'" + VALUES_SHEET + "'!$" + column + "$2:$" + column + "$" + lastRow);
    }

    private static void createIncomesSheet(Workbook workbook, Sheet sheet, CreationHelper creationHelper) {
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
            row.createCell(0).setCellStyle(dateStyle);
            row.createCell(3).setCellStyle(amountStyle);
        }

        addDateValidation(sheet);
        addAmountValidation(sheet);
        addFormulaListValidation(sheet, 2, CATEGORY_RANGE, "Categoria invalida", "Use una categoria INCOME activa de la hoja Valores.");
        addFormulaListValidation(sheet, 4, PARTICIPANT_RANGE, "Participante invalido", "Use un participante activo de la hoja Valores o deje vacio.");

        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 36 * 256);
        sheet.setColumnWidth(2, 30 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.setColumnWidth(4, 42 * 256);
    }

    private static void addDateValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createDateConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "DATE(2000,1,1)",
                "DATE(2100,12,31)",
                "yyyy-mm-dd"
        );
        DataValidation validation = helper.createValidation(constraint, rows(0));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Fecha invalida", "Use formato yyyy-mm-dd.");
        sheet.addValidationData(validation);
    }

    private static void addAmountValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createDecimalConstraint(
                DataValidationConstraint.OperatorType.GREATER_THAN,
                "0",
                null
        );
        DataValidation validation = helper.createValidation(constraint, rows(3));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Monto invalido", "Use un numero mayor a 0.");
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
