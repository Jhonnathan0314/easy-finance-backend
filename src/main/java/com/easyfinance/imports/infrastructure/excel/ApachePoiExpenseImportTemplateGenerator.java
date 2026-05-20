package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.port.out.ExpenseImportTemplateGeneratorPort;
import com.easyfinance.imports.application.template.ExpenseImportTemplateData;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
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
public class ApachePoiExpenseImportTemplateGenerator implements ExpenseImportTemplateGeneratorPort {

    private static final int MAX_DATA_ROWS = 1000;
    private static final String EXPENSES_SHEET = "Gastos";
    private static final String VALUES_SHEET = "Valores";
    private static final String CATEGORY_RANGE = "CategoriasGastos";
    private static final String PAYMENT_METHOD_RANGE = "MediosPago";
    private static final String PAYMENT_STATE_RANGE = "EstadosPago";
    private static final String DEBT_RANGE = "DeudasActivas";
    private static final String DEBT_PAYMENT_FLAG_RANGE = "AplicaPagoDeudaValores";
    private static final String DEBT_PAYMENT_TYPE_RANGE = "TiposPagoDeuda";
    private static final String[] HEADERS = {"Fecha", "DescripciÃ³n", "Monto", "CategorÃ­a", "MedioPago", "EstadoPago", "AplicaPagoDeuda", "Deuda", "TipoPagoDeuda", "NotasPagoDeuda"};
    private static final String[] PAYMENT_STATES = {"PENDING", "PARTIAL", "PAID"};
    private static final String[] DEBT_PAYMENT_FLAGS = {"SI", "NO"};
    private static final String[] DEBT_PAYMENT_TYPES = {"INSTALLMENT", "CAPITAL_PAYMENT"};

    @Override
    public byte[] generate(ExpenseImportTemplateData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet expenses = workbook.createSheet(EXPENSES_SHEET);
            Sheet values = workbook.createSheet(VALUES_SHEET);
            CreationHelper creationHelper = workbook.getCreationHelper();

            createValuesSheet(values, data);
            createNamedRanges(workbook, data.categoryNames().size(), data.paymentMethodNames().size(), data.debtOptions().size());
            createExpensesSheet(workbook, expenses, creationHelper);
            workbook.setSheetHidden(workbook.getSheetIndex(values), true);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Expense import template could not be generated.", ex);
        }
    }

    private static void createExpensesSheet(Workbook workbook, Sheet sheet, CreationHelper creationHelper) {
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
            addHeaderComment(sheet, cell, creationHelper, commentFor(i));
        }

        for (int rowIndex = 1; rowIndex <= MAX_DATA_ROWS; rowIndex++) {
            Row row = sheet.createRow(rowIndex);
            row.createCell(0).setCellStyle(dateStyle);
            row.createCell(2).setCellStyle(amountStyle);
        }

        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 36 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 28 * 256);
        sheet.setColumnWidth(4, 24 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 56 * 256);
        sheet.setColumnWidth(8, 22 * 256);
        sheet.setColumnWidth(9, 32 * 256);

        addDateValidation(sheet);
        addAmountValidation(sheet);
        addFormulaListValidation(sheet, 3, CATEGORY_RANGE, "CategorÃ­a invÃ¡lida", "Use una categorÃ­a EXPENSE activa de la hoja Valores.");
        addFormulaListValidation(sheet, 4, PAYMENT_METHOD_RANGE, "Medio de pago invÃ¡lido", "Use un medio de pago activo de la hoja Valores.");
        addFormulaListValidation(sheet, 5, PAYMENT_STATE_RANGE, "Estado invÃ¡lido", "Use PENDING, PARTIAL o PAID.");
        addFormulaListValidation(sheet, 6, DEBT_PAYMENT_FLAG_RANGE, "Valor invÃ¡lido", "Use SI o NO.");
        addFormulaListValidation(sheet, 7, DEBT_RANGE, "Deuda invÃ¡lida", "Use una deuda ACTIVE de la hoja Valores.");
        addFormulaListValidation(sheet, 8, DEBT_PAYMENT_TYPE_RANGE, "Tipo invÃ¡lido", "Use INSTALLMENT o CAPITAL_PAYMENT.");
    }

    private static void createValuesSheet(Sheet sheet, ExpenseImportTemplateData data) {
        List<String> categoryNames = data.categoryNames();
        List<String> paymentMethodNames = data.paymentMethodNames();
        List<ExpenseImportTemplateData.DebtOption> debtOptions = data.debtOptions();
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("CategorÃ­as");
        header.createCell(1).setCellValue("MediosPago");
        header.createCell(2).setCellValue("EstadosPago");
        header.createCell(3).setCellValue("Deudas");
        header.createCell(4).setCellValue("DeudaId");
        header.createCell(5).setCellValue("AplicaPagoDeuda");
        header.createCell(6).setCellValue("TiposPagoDeuda");
        header.createCell(8).setCellValue("Instrucciones");
        header.createCell(9).setCellValue("No modificar las cabeceras de la hoja Gastos. MÃ¡ximo 1000 filas. CategorÃ­as, medios y deudas deben existir activos.");

        int maxRows = max(categoryNames.size(), paymentMethodNames.size(), PAYMENT_STATES.length, debtOptions.size(), DEBT_PAYMENT_FLAGS.length, DEBT_PAYMENT_TYPES.length);
        for (int i = 0; i < Math.max(maxRows, 1); i++) {
            Row row = sheet.createRow(i + 1);
            if (i < categoryNames.size()) {
                row.createCell(0).setCellValue(categoryNames.get(i));
            }
            if (i < paymentMethodNames.size()) {
                row.createCell(1).setCellValue(paymentMethodNames.get(i));
            }
            if (i < PAYMENT_STATES.length) {
                row.createCell(2).setCellValue(PAYMENT_STATES[i]);
            }
            if (i < debtOptions.size()) {
                row.createCell(3).setCellValue(debtOptions.get(i).label());
                row.createCell(4).setCellValue(debtOptions.get(i).debtId());
            }
            if (i < DEBT_PAYMENT_FLAGS.length) {
                row.createCell(5).setCellValue(DEBT_PAYMENT_FLAGS[i]);
            }
            if (i < DEBT_PAYMENT_TYPES.length) {
                row.createCell(6).setCellValue(DEBT_PAYMENT_TYPES[i]);
            }
        }
        if (categoryNames.isEmpty()) {
            sheet.getRow(1).createCell(8).setCellValue("No hay categorÃ­as EXPENSE activas para esta cuenta.");
        }
        if (paymentMethodNames.isEmpty()) {
            sheet.getRow(1).createCell(9).setCellValue("No hay medios de pago activos para esta cuenta.");
        }
        if (debtOptions.isEmpty()) {
            sheet.getRow(1).createCell(10).setCellValue("No hay deudas ACTIVE para esta cuenta.");
        }

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 58 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        sheet.setColumnWidth(5, 18 * 256);
        sheet.setColumnWidth(6, 22 * 256);
        sheet.setColumnWidth(8, 18 * 256);
        sheet.setColumnWidth(9, 90 * 256);
        sheet.setColumnWidth(10, 40 * 256);
    }

    private static void createNamedRanges(Workbook workbook, int categoryCount, int paymentMethodCount, int debtCount) {
        createNamedRange(workbook, CATEGORY_RANGE, 0, Math.max(categoryCount, 1));
        createNamedRange(workbook, PAYMENT_METHOD_RANGE, 1, Math.max(paymentMethodCount, 1));
        createNamedRange(workbook, PAYMENT_STATE_RANGE, 2, PAYMENT_STATES.length);
        createNamedRange(workbook, DEBT_RANGE, 3, Math.max(debtCount, 1));
        createNamedRange(workbook, DEBT_PAYMENT_FLAG_RANGE, 5, DEBT_PAYMENT_FLAGS.length);
        createNamedRange(workbook, DEBT_PAYMENT_TYPE_RANGE, 6, DEBT_PAYMENT_TYPES.length);
    }

    private static void createNamedRange(Workbook workbook, String name, int columnIndex, int valueCount) {
        Name range = workbook.createName();
        range.setNameName(name);
        char column = (char) ('A' + columnIndex);
        int lastRow = valueCount + 1;
        range.setRefersToFormula("'" + VALUES_SHEET + "'!$" + column + "$2:$" + column + "$" + lastRow);
    }

    private static void addFormulaListValidation(Sheet sheet, int columnIndex, String rangeName, String title, String message) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        DataValidation validation = helper.createValidation(constraint, rows(columnIndex));
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
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
        validation.createErrorBox("Fecha invÃ¡lida", "Use formato yyyy-mm-dd.");
        sheet.addValidationData(validation);
    }

    private static void addAmountValidation(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createDecimalConstraint(
                DataValidationConstraint.OperatorType.GREATER_THAN,
                "0",
                null
        );
        DataValidation validation = helper.createValidation(constraint, rows(2));
        validation.setShowErrorBox(true);
        validation.createErrorBox("Monto invÃ¡lido", "Use un nÃºmero mayor a 0.");
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

    private static void addHeaderComment(Sheet sheet, Cell cell, CreationHelper creationHelper, String text) {
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = creationHelper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 4);
        anchor.setRow1(0);
        anchor.setRow2(4);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(creationHelper.createRichTextString(text));
        cell.setCellComment(comment);
    }

    private static String commentFor(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> "Fecha del gasto. Formato yyyy-mm-dd.";
            case 1 -> "DescripciÃ³n libre del gasto. No modificar esta cabecera.";
            case 2 -> "Monto del gasto. Debe ser mayor a 0.";
            case 3 -> "Seleccione una categorÃ­a EXPENSE activa de esta cuenta.";
            case 4 -> "Seleccione un medio de pago activo de esta cuenta.";
            case 5 -> "Seleccione PENDING, PARTIAL o PAID.";
            case 6 -> "Use SI si esta fila tambien registra pago de deuda; vacio equivale a NO.";
            case 7 -> "Seleccione una deuda ACTIVE de esta cuenta cuando AplicaPagoDeuda sea SI.";
            case 8 -> "Seleccione INSTALLMENT o CAPITAL_PAYMENT cuando AplicaPagoDeuda sea SI.";
            case 9 -> "Notas opcionales para el pago de deuda.";
            default -> "MÃ¡ximo 1000 filas.";
        };
    }

    private static int max(int... values) {
        int result = 0;
        for (int value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}
