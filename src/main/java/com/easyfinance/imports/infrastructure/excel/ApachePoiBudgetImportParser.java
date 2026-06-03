package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.port.out.AnnualBudgetImportParserPort;
import com.easyfinance.imports.application.validation.AnnualBudgetImportMonthScope;
import com.easyfinance.imports.application.validation.AnnualBudgetImportParsedRow;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ApachePoiBudgetImportParser implements AnnualBudgetImportParserPort {

    private static final List<String> REQUIRED_HEADERS = List.of("Año", "NombreSubpresupuesto", "Categoria", "Valor");
    private final int maxRows;

    public ApachePoiBudgetImportParser(@Value("${easy-finance.imports.budgets-annual.max-rows:1000}") int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<AnnualBudgetImportParsedRow> parse(ImportAnnualBudgetCommand command) {
        try (Workbook workbook = new XSSFWorkbook(command.inputStream())) {
            Sheet sheet = workbook.getSheet("PresupuestoAnual");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template is invalid.");
            }
            Map<String, Integer> columns = resolveColumns(sheet.getRow(0));
            List<AnnualBudgetImportParsedRow> rows = new ArrayList<>();
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
        if (!columns.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template header is invalid.");
        }
        return columns;
    }

    private AnnualBudgetImportParsedRow parseRow(Row row, Map<String, Integer> columns) {
        List<String> errors = new ArrayList<>();

        Integer year = readYear(row.getCell(columns.get("Año")), errors);
        AnnualBudgetImportMonthScope monthScope = parseMonth(columns.containsKey("Mes") ? row.getCell(columns.get("Mes")) : null, errors);
        String budgetName = optionalText(columns.containsKey("NombrePresupuesto") ? row.getCell(columns.get("NombrePresupuesto")) : null);
        String categoryName = requiredText(row.getCell(columns.get("Categoria")), "Categoria", errors);
        String subBudgetName = requiredText(row.getCell(columns.get("NombreSubpresupuesto")), "NombreSubpresupuesto", errors);
        BigDecimal value = readPositiveDecimal(row.getCell(columns.get("Valor")), errors);

        if (subBudgetName != null && subBudgetName.length() > 150) {
            errors.add("NombreSubpresupuesto supera el máximo permitido");
        }

        return new AnnualBudgetImportParsedRow(
                row.getRowNum() + 1,
                year,
                monthScope,
                budgetName,
                categoryName,
                subBudgetName,
                value,
                errors
        );
    }

    private Integer readYear(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add("Año requerido");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Año inválido");
            return null;
        }
        try {
            int year = cell.getCellType() == CellType.NUMERIC
                    ? (int) Math.round(cell.getNumericCellValue())
                    : Integer.parseInt(text(cell).trim());
            if (year < 2000 || year > 2100) {
                errors.add("Año fuera de rango");
                return null;
            }
            return year;
        } catch (RuntimeException ex) {
            errors.add("Año inválido");
            return null;
        }
    }

    private AnnualBudgetImportMonthScope parseMonth(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            return new AnnualBudgetImportMonthScope.AllMonths();
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Mes inválido");
            return new AnnualBudgetImportMonthScope.AllMonths();
        }
        String raw = text(cell).trim();
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.equals("todos")) {
            return new AnnualBudgetImportMonthScope.AllMonths();
        }
        Integer monthByName = switch (normalized) {
            case "enero" -> 1;
            case "febrero" -> 2;
            case "marzo" -> 3;
            case "abril" -> 4;
            case "mayo" -> 5;
            case "junio" -> 6;
            case "julio" -> 7;
            case "agosto" -> 8;
            case "septiembre" -> 9;
            case "octubre" -> 10;
            case "noviembre" -> 11;
            case "diciembre" -> 12;
            default -> null;
        };
        if (monthByName != null) {
            return new AnnualBudgetImportMonthScope.SingleMonth(monthByName);
        }
        try {
            int month = Integer.parseInt(raw);
            if (month < 1 || month > 12) {
                errors.add("Mes inválido");
                return new AnnualBudgetImportMonthScope.AllMonths();
            }
            return new AnnualBudgetImportMonthScope.SingleMonth(month);
        } catch (RuntimeException ex) {
            errors.add("Mes inválido");
            return new AnnualBudgetImportMonthScope.AllMonths();
        }
    }

    private BigDecimal readPositiveDecimal(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add("Valor requerido");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("Valor inválido");
            return null;
        }
        try {
            BigDecimal value = cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue())
                    : new BigDecimal(text(cell).trim());
            if (value.signum() <= 0) {
                errors.add("Valor debe ser mayor a 0");
                return null;
            }
            return value;
        } catch (RuntimeException ex) {
            errors.add("Valor inválido");
            return null;
        }
    }

    private String requiredText(Cell cell, String field, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add(field + " requerido");
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(field + " requerido");
            return null;
        }
        return text(cell).trim();
    }

    private String optionalText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return null;
        }
        String value = text(cell).trim();
        return value.isEmpty() ? null : value;
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
}

