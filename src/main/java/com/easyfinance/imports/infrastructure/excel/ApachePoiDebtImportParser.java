package com.easyfinance.imports.infrastructure.excel;

import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.port.out.DebtImportParserPort;
import com.easyfinance.imports.application.validation.DebtImportParsedRow;
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
public class ApachePoiDebtImportParser implements DebtImportParserPort {

    private static final String NAME_HEADER = "Nombre";
    private static final String DESCRIPTION_HEADER = "Descripcion";
    private static final String TOTAL_AMOUNT_HEADER = "Capital";
    private static final String REMAINING_BALANCE_HEADER = "SaldoPendiente";
    private static final String INSTALLMENT_COUNT_HEADER = "NumeroCuotas";
    private static final String INSTALLMENT_AMOUNT_HEADER = "ValorCuota";
    private static final String START_DATE_HEADER = "FechaInicio";
    private static final String DUE_DATE_HEADER = "FechaVencimiento";
    private static final String PARTICIPANT_HEADER = "Participante";
    private static final String NOTES_HEADER = "Notas";
    private static final List<String> REQUIRED_HEADERS = List.of(NAME_HEADER, TOTAL_AMOUNT_HEADER, START_DATE_HEADER);

    private final int maxRows;

    public ApachePoiDebtImportParser(@Value("${easy-finance.imports.debts.max-rows:1000}") int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public List<DebtImportParsedRow> parse(ImportDebtCommand command, Long accountId) {
        try (Workbook workbook = new XSSFWorkbook(command.inputStream())) {
            Sheet sheet = workbook.getSheet("Deudas");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new BusinessRuleViolationException("IMPORT_TEMPLATE_INVALID", "Import template is invalid.");
            }
            Map<String, Integer> columns = resolveColumns(sheet.getRow(0));
            List<DebtImportParsedRow> rows = new ArrayList<>();
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

    private DebtImportParsedRow parseRow(Row row, Map<String, Integer> columns) {
        List<String> errors = new ArrayList<>();
        String name = requiredText(row.getCell(columns.get(NAME_HEADER)), "Nombre", errors);
        String description = optionalText(optionalCell(row, columns, DESCRIPTION_HEADER));
        BigDecimal totalAmount = readAmount(row.getCell(columns.get(TOTAL_AMOUNT_HEADER)), "Capital", true, errors);
        BigDecimal remainingBalance = readAmount(optionalCell(row, columns, REMAINING_BALANCE_HEADER), "SaldoPendiente", false, errors);
        Integer installmentCount = readInstallmentCount(optionalCell(row, columns, INSTALLMENT_COUNT_HEADER), errors);
        BigDecimal installmentAmount = readAmount(optionalCell(row, columns, INSTALLMENT_AMOUNT_HEADER), "ValorCuota", false, errors);
        LocalDate startDate = readDate(row.getCell(dateColumn(columns, START_DATE_HEADER)), "FechaInicio", true, errors);
        LocalDate dueDate = readDate(optionalCell(row, columns, DUE_DATE_HEADER), "FechaVencimiento", false, errors);
        String participant = optionalText(optionalCell(row, columns, PARTICIPANT_HEADER));
        String notes = optionalText(optionalCell(row, columns, NOTES_HEADER));

        if ((installmentCount == null) != (installmentAmount == null)) {
            errors.add("Numero de cuotas y valor de cuota deben completarse juntos");
        }
        if (remainingBalance != null && totalAmount != null && remainingBalance.compareTo(totalAmount) > 0) {
            errors.add("Saldo pendiente no puede superar el capital");
        }

        String status = columns.containsKey("Estado") ? optionalText(optionalCell(row, columns, "Estado")) : null;
        return new DebtImportParsedRow(row.getRowNum() + 1, name, description, totalAmount, remainingBalance, installmentCount, installmentAmount, startDate, dueDate, participant, notes, status, errors);
    }

    private BigDecimal readAmount(Cell cell, String field, boolean required, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            if (required) {
                errors.add(field + " requerido");
            }
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(field + " invalido");
            return null;
        }
        try {
            BigDecimal value = cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue())
                    : new BigDecimal(text(cell));
            if (value.signum() <= 0) {
                errors.add(field + " debe ser mayor a 0");
                return null;
            }
            return value;
        } catch (RuntimeException ex) {
            errors.add(field + " invalido");
            return null;
        }
    }

    private Integer readInstallmentCount(Cell cell, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add("NumeroCuotas invalido");
            return null;
        }
        try {
            int value = cell.getCellType() == CellType.NUMERIC
                    ? (int) cell.getNumericCellValue()
                    : Integer.parseInt(text(cell).trim());
            if (value <= 0) {
                errors.add("NumeroCuotas debe ser mayor a 0");
                return null;
            }
            return value;
        } catch (RuntimeException ex) {
            errors.add("NumeroCuotas invalido");
            return null;
        }
    }

    private LocalDate readDate(Cell cell, String field, boolean required, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            if (required) {
                errors.add(field + " requerida");
            }
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            errors.add(field + " invalida");
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        try {
            return LocalDate.parse(text(cell));
        } catch (RuntimeException ex) {
            errors.add(field + " invalida");
            return null;
        }
    }

    private String requiredText(Cell cell, String field, List<String> errors) {
        if (cell == null || cell.getCellType() == CellType.BLANK || cell.getCellType() == CellType.FORMULA || text(cell).isBlank()) {
            errors.add(field + " requerido");
            return null;
        }
        return text(cell).trim();
    }

    private String optionalText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK || cell.getCellType() == CellType.FORMULA) {
            return null;
        }
        String value = text(cell).trim();
        return value.isBlank() ? null : value;
    }

    private static Cell optionalCell(Row row, Map<String, Integer> columns, String header) {
        Integer column = columns.get(header);
        return column == null ? null : row.getCell(column);
    }

    private static Integer dateColumn(Map<String, Integer> columns, String header) {
        return columns.get(header);
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
