package com.easyfinance.imports.application;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.shared.application.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class ExportService {
    private static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final EntityManager em;
    private final AccountAuthorizationService auth;
    private final CurrentUserProvider currentUser;

    public ExportService(EntityManager em, AccountAuthorizationService auth, CurrentUserProvider currentUser) {
        this.em = em; this.auth = auth; this.currentUser = currentUser;
    }

    public ExportFile export(Long accountId, String module, Integer year, Integer month) {
        auth.requireActiveAdminForActiveAccount(accountId, currentUser.currentUser().orElseThrow().participantId());
        if (year != null && (year < 2000 || year > 2100)) throw new IllegalArgumentException("Invalid year");
        if (month != null && (month < 1 || month > 12)) throw new IllegalArgumentException("Invalid month");
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = switch (module) { case "incomes" -> "Ingresos"; case "expenses" -> "Gastos"; case "debts" -> "Deudas"; case "categories" -> "Categorias"; case "payment-methods" -> "MediosPago"; case "budgets" -> "PresupuestoAnual"; default -> throw new IllegalArgumentException("Unknown export module"); };
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            dateStyle.setLocked(false);
            String[] headers = headers(module);
            Row h = sheet.createRow(0); for (int i=0;i<headers.length;i++) { var c=h.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(headerStyle); }
            List<Object[]> rows = rows(accountId, module, year, month);
            for (int i=0;i<rows.size();i++) { Row r=sheet.createRow(i+1); Object[] data=rows.get(i); for(int j=0;j<data.length;j++) if(data[j]!=null) { var c=r.createCell(j); if(data[j] instanceof Number n) c.setCellValue(n.doubleValue()); else if(data[j] instanceof LocalDate d) { c.setCellValue(d); c.setCellStyle(dateStyle); } else { c.setCellValue(data[j].toString()); var editable=wb.createCellStyle(); editable.setLocked(false); c.setCellStyle(editable); } } }
            for(int i=0;i<headers.length;i++) sheet.autoSizeColumn(i);
            sheet.createFreezePane(0, 1);
            if (!rows.isEmpty()) sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, headers.length - 1));
            addDynamicValidations(wb, sheet, accountId, module);
            wb.write(out);
            return new ExportFile(filename(module, year, month), TYPE, out.toByteArray());
        } catch (Exception e) { throw new IllegalStateException("Could not generate export", e); }
    }

    private String filename(String module, Integer year, Integer month) {
        String base = "easy-finance-" + module + "-export";
        if ((module.equals("incomes") || module.equals("expenses") || module.equals("budgets")) && year != null) {
            base += "-" + year;
            if (month != null) base += "-" + String.format("%02d", month);
        }
        return base + ".xlsx";
    }

    private void addDynamicValidations(Workbook wb, Sheet sheet, Long accountId, String module) {
        Sheet values = wb.createSheet("Valores");
        Map<String, List<String>> lists = new LinkedHashMap<>();
        if (module.equals("incomes") || module.equals("expenses") || module.equals("budgets")) {
            String type = module.equals("incomes") ? "INCOME" : "EXPENSE";
            lists.put("Categorias", strings("select name from categories where account_id=:a and type=:t order by name", accountId, type));
        }
        if (module.equals("expenses")) lists.put("MediosPago", strings("select name from payment_methods where account_id=:a order by name", accountId));
        if (module.equals("expenses")) lists.put("Deudas", strings("select name from debts where account_id=:a order by name", accountId));
        if (module.equals("incomes") || module.equals("expenses") || module.equals("debts") || module.equals("budgets")) lists.put("Participantes", strings("select display_name from participants where id in (select participant_id from account_participants where account_id=:a and status='ACTIVE') order by display_name", accountId));
        if (module.equals("incomes") || module.equals("expenses") || module.equals("debts") || module.equals("categories") || module.equals("payment-methods") || module.equals("budgets")) lists.put("Estados", module.equals("incomes") ? List.of("ACTIVE","CANCELLED") : module.equals("expenses") ? List.of("ACTIVE","CANCELLED") : module.equals("debts") ? List.of("ACTIVE","CANCELLED") : module.equals("budgets") ? List.of("ACTIVE","INACTIVE") : List.of("ACTIVE","INACTIVE"));
        if (module.equals("expenses")) { lists.put("EstadosPago", List.of("PENDING","PARTIAL","PAID")); lists.put("SiNo", List.of("SI","NO")); lists.put("TiposPagoDeuda", List.of("CAPITAL","INTEREST","MIXED")); }
        if (module.equals("categories")) lists.put("Tipos", List.of("Gasto","Ingreso","EXPENSE","INCOME"));
        if (module.equals("payment-methods")) lists.put("Tipos", List.of("Efectivo","CuentaBancaria","TarjetaCredito","TarjetaDebito","BilleteraDigital","Otro","CASH","BANK_ACCOUNT","CREDIT_CARD","DEBIT_CARD","DIGITAL_WALLET","OTHER"));
        int column = 0;
        for (var entry : lists.entrySet()) {
            Row h = values.getRow(0); if (h == null) h = values.createRow(0); h.createCell(column).setCellValue(entry.getKey());
            for (int i=0;i<entry.getValue().size();i++) {
                Row valueRow = values.getRow(i + 1);
                if (valueRow == null) valueRow = values.createRow(i + 1);
                valueRow.createCell(column).setCellValue(entry.getValue().get(i));
            }
            var name = wb.createName(); name.setNameName(entry.getKey()); name.setRefersToFormula("'Valores'!$" + (char)('A'+column) + "$2:$" + (char)('A'+column) + "$" + Math.max(2, entry.getValue().size()+1));
            Integer target = validationColumn(module, entry.getKey()); if (target != null && !entry.getValue().isEmpty()) addListValidation(sheet, target, entry.getKey());
            column++;
        }
        wb.setSheetHidden(wb.getSheetIndex(values), true);
    }

    private List<String> strings(String sql, Long accountId, String... type) {
        var q = em.createNativeQuery(sql).setParameter("a", accountId); if (type.length > 0) q.setParameter("t", type[0]);
        return ((List<?>) q.getResultList()).stream().map(Object::toString).toList();
    }

    private void addListValidation(Sheet sheet, int column, String range) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createFormulaListConstraint(range);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 5000, column, column));
        validation.setShowErrorBox(false);
        sheet.addValidationData(validation);
    }

    private Integer validationColumn(String module, String list) {
        return switch (module + ":" + list) {
            case "incomes:Categorias" -> 2; case "incomes:Participantes" -> 4; case "incomes:Estados" -> 5;
            case "expenses:Categorias" -> 3; case "expenses:MediosPago" -> 4; case "expenses:EstadosPago" -> 5; case "expenses:SiNo" -> 6; case "expenses:Deudas" -> 7; case "expenses:TiposPagoDeuda" -> 8; case "expenses:Participantes" -> 10; case "expenses:Estados" -> 11;
            case "debts:Participantes" -> 8; case "debts:Estados" -> 10;
            case "categories:Tipos" -> 1; case "categories:Estados" -> 3;
            case "payment-methods:Tipos" -> 1; case "payment-methods:Estados" -> 3;
            case "budgets:Categorias" -> 3; case "budgets:Participantes" -> 6; case "budgets:Estados" -> 7;
            default -> null;
        };
    }

    private List<Object[]> rows(Long a, String m, Integer y, Integer mo) {
        String period = (y == null ? "" : " and extract(year from x_date)=:y") + (mo == null ? "" : " and extract(month from x_date)=:m");
        String sql;
        switch(m) {
            case "incomes" -> sql="select x.income_date,x.description,c.name,x.amount,p.display_name,x.status from incomes x join categories c on c.id=x.category_id join participants p on p.id=x.participant_id where x.account_id=:a" + period.replace("x_date","x.income_date") + " order by x.income_date,x.id";
            case "expenses" -> sql="select x.expense_date,x.description,x.amount,c.name,pm.name,x.payment_state,'NO',null,null,null,p.display_name,x.status from expenses x join categories c on c.id=x.category_id join payment_methods pm on pm.id=x.payment_method_id join participants p on p.id=x.participant_id where x.account_id=:a" + period.replace("x_date","x.expense_date") + " order by x.expense_date,x.id";
            case "debts" -> sql="select x.name,x.description,x.total_amount,x.remaining_amount,x.installment_count,x.installment_amount,x.start_date,x.end_date,p.display_name,x.notes,x.state from debts x join participants p on p.id=x.participant_id where x.account_id=:a order by x.start_date,x.id";
            case "categories" -> sql="select name,type,description,status from categories where account_id=:a order by name";
            case "payment-methods" -> sql="select name,type,description,status from payment_methods where account_id=:a order by name";
            case "budgets" -> sql="select b.year,b.month,b.name,c.name,s.name,s.planned_amount,p.display_name,s.status from budgets b join sub_budgets s on s.budget_id=b.id left join categories c on c.id=s.category_id left join participants p on p.id=s.participant_id where b.account_id=:a" + (y==null?"":" and b.year=:y") + (mo==null?"":" and b.month=:m") + " order by b.year,b.month,s.id";
            default -> throw new IllegalArgumentException();
        }
        var q=em.createNativeQuery(sql).setParameter("a",a); if(y!=null)q.setParameter("y",y); if(mo!=null)q.setParameter("m",mo); return q.getResultList();
    }
    private String[] headers(String m) { return switch(m) { case "incomes" -> new String[]{"Fecha (yyyy-MM-dd)","Descripcion","Categoria","Monto","Participante","Estado"}; case "expenses" -> new String[]{"Fecha","DescripciÃ³n","Monto","CategorÃ­a","MedioPago","EstadoPago","AplicaPagoDeuda","Deuda","TipoPagoDeuda","NotasPagoDeuda","Participante","Estado"}; case "debts" -> new String[]{"Nombre","Descripcion","Capital","SaldoPendiente","NumeroCuotas","ValorCuota","FechaInicio","FechaVencimiento","Participante","Notas","Estado"}; case "categories","payment-methods" -> new String[]{"Nombre","Tipo","Descripcion","Estado"}; case "budgets" -> new String[]{"Año","Mes","NombrePresupuesto","Categoria","NombreSubpresupuesto","Valor","Participante","Estado"}; default -> new String[0]; }; }
    public record ExportFile(String filename,String contentType,byte[] content) {}
}
