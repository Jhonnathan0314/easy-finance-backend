package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.port.out.AnnualBudgetImportTemplateGeneratorPort;
import com.easyfinance.imports.application.template.AnnualBudgetImportTemplateData;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
public class ApachePoiBudgetImportTemplateGenerator implements AnnualBudgetImportTemplateGeneratorPort {

    private static final int MAX_DATA_ROWS = 1000;
    private static final String SHEET_NAME = "PresupuestoAnual";
    private static final String VALUES_SHEET = "Valores";
    private static final String CATEGORY_RANGE = "CategoriasPresupuesto";
    private static final String MONTH_RANGE = "MesesPresupuesto";
    private static final String[] MONTHS = {"Todos", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final String[] HEADERS = {"Año", "Mes", "NombrePresupuesto", "Categoria", "NombreSubpresupuesto", "Valor"};

    @Override
    public byte[] generate(AnnualBudgetImportTemplateData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet budgetSheet = workbook.createSheet(SHEET_NAME);
            Sheet values = workbook.createSheet(VALUES_SHEET);

            createValuesSheet(values, data.expenseCategoryNames());
            createNamedRanges(workbook, data.expenseCategoryNames().size());
            createBudgetSheet(workbook, budgetSheet);
            workbook.setSheetHidden(workbook.getSheetIndex(values), true);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Annual budget import template could not be generated.", ex);
        }
    }

    private static void createValuesSheet(Sheet sheet, List<String> categories) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Meses");
        header.createCell(2).setCellValue("Categorias");
        header.createCell(4).setCellValue("Instrucciones");
        header.createCell(5).setCellValue("No modificar cabeceras. Maximo 1000 filas.");

        for (int i = 0; i < MONTHS.length; i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) {
                row = sheet.createRow(i + 1);
            }
            row.createCell(0).setCellValue(MONTHS[i]);
        }

        int maxRows = Math.max(categories.size(), 1);
        for (int i = 0; i < maxRows; i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) {
                row = sheet.createRow(i + 1);
            }
            if (i < categories.size()) {
                row.createCell(2).setCellValue(categories.get(i));
            }
        }
        if (categories.isEmpty()) {
            Row row = sheet.getRow(1);
            if (row == null) {
                row = sheet.createRow(1);
            }
            row.createCell(4).setCellValue("No hay categorias EXPENSE activas para esta cuenta.");
        }

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(2, 36 * 256);
        sheet.setColumnWidth(4, 18 * 256);
        sheet.setColumnWidth(5, 70 * 256);
    }

    private static void createNamedRanges(Workbook workbook, int categoryCount) {
        createNamedRange(workbook, MONTH_RANGE, 'A', MONTHS.length + 1);
        createNamedRange(workbook, CATEGORY_RANGE, 'C', Math.max(categoryCount, 1) + 1);
    }

    private static void createNamedRange(Workbook workbook, String rangeName, char column, int endRow) {
        org.apache.poi.ss.usermodel.Name range = workbook.createName();
        range.setNameName(rangeName);
        range.setRefersToFormula("'" + VALUES_SHEET + "'!$" + column + "$2:$" + column + "$" + endRow);
    }

    private static void createBudgetSheet(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = headerStyle(workbook);
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        addMonthValidation(sheet);
        addCategoryValidation(sheet);

        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 36 * 256);
        sheet.setColumnWidth(3, 36 * 256);
        sheet.setColumnWidth(4, 36 * 256);
        sheet.setColumnWidth(5, 16 * 256);
    }

    private static void addMonthValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(MONTH_RANGE);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_DATA_ROWS, 1, 1));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Mes invalido", "Use Todos o un mes valido en español.");
        sheet.addValidationData(validation);
    }

    private static void addCategoryValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(CATEGORY_RANGE);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_DATA_ROWS, 3, 3));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Categoria invalida", "Use una categoria EXPENSE activa.");
        sheet.addValidationData(validation);
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

