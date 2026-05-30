package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.port.out.CategoryImportTemplateGeneratorPort;
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

@Component
public class ApachePoiCategoryImportTemplateGenerator implements CategoryImportTemplateGeneratorPort {

    private static final int MAX_DATA_ROWS = 1000;
    private static final String CATEGORIES_SHEET = "Categorias";
    private static final String VALUES_SHEET = "Valores";
    private static final String TYPE_RANGE = "TiposCategoria";
    private static final String[] HEADERS = {"Nombre", "Tipo"};

    @Override
    public byte[] generate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet categories = workbook.createSheet(CATEGORIES_SHEET);
            Sheet values = workbook.createSheet(VALUES_SHEET);

            createValuesSheet(values);
            createNamedRange(workbook);
            createCategoriesSheet(workbook, categories);
            workbook.setSheetHidden(workbook.getSheetIndex(values), true);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Category import template could not be generated.", ex);
        }
    }

    private static void createValuesSheet(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Tipos");
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Gasto");
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Ingreso");
        header.createCell(2).setCellValue("Instrucciones");
        header.createCell(3).setCellValue("No modificar cabeceras. Maximo 1000 filas.");
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 70 * 256);
    }

    private static void createNamedRange(Workbook workbook) {
        var range = workbook.createName();
        range.setNameName(TYPE_RANGE);
        range.setRefersToFormula("'" + VALUES_SHEET + "'!$A$2:$A$3");
    }

    private static void createCategoriesSheet(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = headerStyle(workbook);
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
        addTypeValidation(sheet);
        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 36 * 256);
        sheet.setColumnWidth(1, 18 * 256);
    }

    private static void addTypeValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(TYPE_RANGE);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_DATA_ROWS, 1, 1));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Tipo invalido", "Use Gasto o Ingreso.");
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

