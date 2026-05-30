package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.port.out.CategoryImportParserPort;
import com.easyfinance.imports.application.validation.CategoryImportParsedRow;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ApachePoiCategoryImportParser implements CategoryImportParserPort {

    private static final List<String> REQUIRED_HEADERS = List.of("Nombre", "Tipo");
    private final int maxRows;

    public ApachePoiCategoryImportParser(@Value("${easy-finance.imports.categories.max-rows:1000}") int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<CategoryImportParsedRow> parse(ImportCategoryCommand command) {
        try (Workbook workbook = new XSSFWorkbook(command.inputStream())) {
            Sheet sheet = workbook.getSheet("Categorias");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template is invalid.");
            }
            Map<String, Integer> columns = resolveColumns(sheet.getRow(0));
            List<CategoryImportParsedRow> rows = new ArrayList<>();
            Set<String> seenKeys = new HashSet<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row)) {
                    continue;
                }
                if (rows.size() >= maxRows) {
                    throw new BusinessRuleViolationException("IMPORT_ROW_LIMIT_EXCEEDED", "Import row limit exceeded.");
                }
                rows.add(parseRow(row, columns, seenKeys));
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

    private CategoryImportParsedRow parseRow(Row row, Map<String, Integer> columns, Set<String> seenKeys) {
        List<String> errors = new ArrayList<>();
        String name = requiredText(row.getCell(columns.get("Nombre")), "Nombre", errors);
        CategoryType type = parseType(row.getCell(columns.get("Tipo")), errors);
        if (errors.isEmpty()) {
            String key = type.name() + "|" + name.trim().toLowerCase(Locale.ROOT);
            if (!seenKeys.add(key)) {
                errors.add("Categoria duplicada dentro del archivo");
            }
        }
        return new CategoryImportParsedRow(row.getRowNum() + 1, name, type, errors);
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

    private CategoryType parseType(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || text(cell).isBlank()) {
            errors.add("Tipo requerido");
            return null;
        }
        String raw = text(cell).trim();
        String normalized = raw.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "GASTO", "EXPENSE" -> CategoryType.EXPENSE;
            case "INGRESO", "INCOME" -> CategoryType.INCOME;
            default -> {
                errors.add("Tipo invalido");
                yield null;
            }
        };
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
