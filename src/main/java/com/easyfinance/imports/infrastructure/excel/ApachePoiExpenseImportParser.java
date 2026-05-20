package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.port.out.ExpenseImportParserPort;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.imports.domain.model.ImportRowError;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ApachePoiExpenseImportParser implements ExpenseImportParserPort {

    private static final List<String> REQUIRED_HEADERS = List.of("Fecha", "Descripción", "Monto", "Categoría", "MedioPago", "EstadoPago");
    private final int maxRows;

    public ApachePoiExpenseImportParser(@Value("${easy-finance.imports.expenses.max-rows:1000}") int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<ExpenseImportRow> parse(PreviewExpenseImportCommand command, Long accountId) {
        try (Workbook workbook = new XSSFWorkbook(command.inputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template is invalid.");
            }
            Map<String, Integer> columns = resolveColumns(sheet.getRow(0));
            List<ExpenseImportRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row sheetRow = sheet.getRow(i);
                if (isBlank(sheetRow)) {
                    continue;
                }
                if (rows.size() >= maxRows) {
                    throw new BusinessRuleViolationException("IMPORT_ROW_LIMIT_EXCEEDED", "Import row limit exceeded.");
                }
                rows.add(parseRow(accountId, sheetRow, columns));
            }
            return rows;
        } catch (BusinessRuleViolationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import file could not be read.", ex);
        }
    }

    private ExpenseImportRow parseRow(Long accountId, Row row, Map<String, Integer> columns) {
        List<ImportRowError> errors = new ArrayList<>();
        LocalDate date = readDate(row.getCell(columns.get("Fecha")), errors);
        String description = requiredText(row.getCell(columns.get("Descripción")), "Descripción", errors);
        Money amount = readAmount(row.getCell(columns.get("Monto")), errors);
        String category = requiredText(row.getCell(columns.get("Categoría")), "Categoría", errors);
        String paymentMethod = requiredText(row.getCell(columns.get("MedioPago")), "MedioPago", errors);
        ExpensePaymentState paymentState = readPaymentState(row.getCell(columns.get("EstadoPago")), errors);
        return new ExpenseImportRow(null, accountId, null, row.getRowNum() + 1, date, description, amount, category, null, paymentMethod, null, paymentState, errors.isEmpty(), errors, null, null, null);
    }

    private Map<String, Integer> resolveColumns(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template header is missing.");
        }
        java.util.HashMap<String, Integer> columns = new java.util.HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            columns.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        }
        if (!columns.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template header is invalid.");
        }
        return columns;
    }

    private LocalDate readDate(Cell cell, List<ImportRowError> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            errors.add(new ImportRowError("Fecha", "REQUIRED", "Fecha is required."));
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(new ImportRowError("Fecha", "INVALID_DATE", "Formula cells are not supported."));
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = text(cell);
        try {
            return LocalDate.parse(text);
        } catch (RuntimeException ex) {
            errors.add(new ImportRowError("Fecha", "INVALID_DATE", "Fecha must be a valid date."));
            return null;
        }
    }

    private Money readAmount(Cell cell, List<ImportRowError> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            errors.add(new ImportRowError("Monto", "REQUIRED", "Monto is required."));
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(new ImportRowError("Monto", "INVALID_AMOUNT", "Formula cells are not supported."));
            return null;
        }
        try {
            BigDecimal value = cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue())
                    : new BigDecimal(text(cell));
            if (value.signum() <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return Money.cop(value);
        } catch (RuntimeException ex) {
            errors.add(new ImportRowError("Monto", "INVALID_AMOUNT", "Monto must be a positive number."));
            return null;
        }
    }

    private ExpensePaymentState readPaymentState(Cell cell, List<ImportRowError> errors) {
        String value = requiredText(cell, "EstadoPago", errors);
        if (value == null) {
            return null;
        }
        try {
            return ExpensePaymentState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            errors.add(new ImportRowError("EstadoPago", "INVALID_PAYMENT_STATE", "EstadoPago is invalid."));
            return null;
        }
    }

    private String requiredText(Cell cell, String column, List<ImportRowError> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add(new ImportRowError(column, "REQUIRED", column + " is required."));
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(new ImportRowError(column, "REQUIRED", "Formula cells are not supported."));
            return null;
        }
        return text(cell).trim();
    }

    private static boolean isBlank(Row row) {
        if (row == null) {
            return true;
        }
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String text(Cell cell) {
        return new DataFormatter().formatCellValue(cell);
    }
}
