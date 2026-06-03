package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.port.out.IncomeImportParserPort;
import com.easyfinance.imports.application.validation.IncomeImportParsedRow;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApachePoiIncomeImportParser implements IncomeImportParserPort {

    private static final String DATE_HEADER = "Fecha";
    private static final String DATE_HEADER_WITH_FORMAT = "Fecha (yyyy-MM-dd)";
    private static final List<String> REQUIRED_HEADERS = List.of("Descripcion", "Categoria", "Monto");
    private final int maxRows;

    public ApachePoiIncomeImportParser(@Value("${easy-finance.imports.incomes.max-rows:1000}") int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<IncomeImportParsedRow> parse(ImportIncomeCommand command, Long accountId) {
        try (Workbook workbook = new XSSFWorkbook(command.inputStream())) {
            Sheet sheet = workbook.getSheet("Ingresos");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template is invalid.");
            }
            Map<String, Integer> columns = resolveColumns(sheet.getRow(0));
            List<IncomeImportParsedRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row)) {
                    continue;
                }
                if (rows.size() >= maxRows) {
                    throw new BusinessRuleViolationException("IMPORT_ROW_LIMIT_EXCEEDED", "Import row limit exceeded.");
                }
                rows.add(parseRow(row, columns));
            }
            return rows;
        } catch (BusinessRuleViolationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import file could not be read.", ex);
        }
    }

    private Map<String, Integer> resolveColumns(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template header is missing.");
        }
        HashMap<String, Integer> columns = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            columns.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        }
        if ((!columns.containsKey(DATE_HEADER) && !columns.containsKey(DATE_HEADER_WITH_FORMAT))
                || !columns.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template header is invalid.");
        }
        return columns;
    }

    private IncomeImportParsedRow parseRow(Row row, Map<String, Integer> columns) {
        List<String> errors = new ArrayList<>();
        LocalDate date = readDate(row.getCell(dateColumn(columns)), errors);
        String description = requiredText(row.getCell(columns.get("Descripcion")), "Descripcion", errors);
        String category = requiredText(row.getCell(columns.get("Categoria")), "Categoria", errors);
        BigDecimal amount = readAmount(row.getCell(columns.get("Monto")), errors);
        if (description != null) {
            description = description.trim();
        }
        return new IncomeImportParsedRow(row.getRowNum() + 1, date, description, category, amount, errors);
    }

    private LocalDate readDate(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            errors.add("Fecha requerida");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Fecha invalida");
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = text(cell);
        try {
            return LocalDate.parse(text);
        } catch (RuntimeException ex) {
            errors.add("Fecha invalida");
            return null;
        }
    }

    private BigDecimal readAmount(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            errors.add("Monto requerido");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Monto invalido");
            return null;
        }
        try {
            BigDecimal value = cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue())
                    : new BigDecimal(text(cell));
            if (value.signum() <= 0) {
                errors.add("Monto debe ser mayor a 0");
                return null;
            }
            return value;
        } catch (RuntimeException ex) {
            errors.add("Monto invalido");
            return null;
        }
    }

    private String requiredText(Cell cell, String field, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add(field + " requerida");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(field + " requerida");
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
        return cell == null ? "" : new DataFormatter().formatCellValue(cell);
    }

    private static Integer dateColumn(Map<String, Integer> columns) {
        Integer column = columns.get(DATE_HEADER);
        return column != null ? column : columns.get(DATE_HEADER_WITH_FORMAT);
    }
}
