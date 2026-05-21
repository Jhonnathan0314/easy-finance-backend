package com.easyfinance.analytics.infrastructure.persistence.jpa;

import com.easyfinance.analytics.application.port.out.AnalyticsQueryPort;
import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesCategoryItem;
import com.easyfinance.analytics.application.response.CashflowItem;
import com.easyfinance.analytics.application.response.CashflowSummaryResponse;
import com.easyfinance.analytics.application.response.CategoryAmountItem;
import com.easyfinance.analytics.application.response.DebtSummaryResponse;
import com.easyfinance.analytics.application.response.ExpenseSummaryResponse;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;
import com.easyfinance.analytics.application.response.PaymentMethodAmountItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JpaAnalyticsQueryAdapter implements AnalyticsQueryPort {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public JpaAnalyticsQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public MonthlySummaryResponse getMonthlySummary(Long accountId, Integer year, Integer month, LocalDate startInclusive, LocalDate endInclusive) {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    COALESCE((SELECT SUM(i.amount) FROM incomes i WHERE i.account_id = ? AND i.status = 'ACTIVE' AND i.income_date BETWEEN ? AND ?), 0) AS total_income,
                    COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE e.account_id = ? AND e.status = 'ACTIVE' AND e.expense_date BETWEEN ? AND ?), 0) AS total_expenses,
                    COALESCE((SELECT SUM(d.remaining_amount) FROM debts d WHERE d.account_id = ? AND d.state = 'ACTIVE'), 0) AS total_debt_remaining,
                    COALESCE((
                        SELECT SUM(dp.amount)
                        FROM debt_payments dp
                        JOIN debts debt ON debt.account_id = dp.account_id AND debt.id = dp.debt_id
                        WHERE dp.account_id = ?
                          AND dp.status = 'ACTIVE'
                          AND dp.payment_date BETWEEN ? AND ?
                          AND debt.state <> 'CANCELLED'
                    ), 0) AS total_debt_paid_in_month,
                    COALESCE((SELECT COUNT(*) FROM debts d WHERE d.account_id = ? AND d.state = 'ACTIVE'), 0) AS active_debts_count,
                    COALESCE((SELECT COUNT(*) FROM debts d WHERE d.account_id = ? AND d.state = 'PAID'), 0) AS paid_debts_count,
                    COALESCE((SELECT SUM(bi.expected_amount) FROM budget_impacts bi WHERE bi.account_id = ? AND bi.period_year = ? AND bi.period_month = ? AND bi.status IN ('ACTIVE', 'PAID')), 0) AS budget_expected,
                    COALESCE((SELECT SUM(bi.paid_amount) FROM budget_impacts bi WHERE bi.account_id = ? AND bi.period_year = ? AND bi.period_month = ? AND bi.status IN ('ACTIVE', 'PAID')), 0) AS budget_paid
                """,
                (rs, rowNum) -> {
                    BigDecimal totalIncome = money(rs, "total_income");
                    BigDecimal totalExpenses = money(rs, "total_expenses");
                    BigDecimal budgetExpected = money(rs, "budget_expected");
                    BigDecimal budgetPaid = money(rs, "budget_paid");
                    return new MonthlySummaryResponse(
                            accountId,
                            year,
                            month,
                            totalIncome,
                            totalExpenses,
                            totalIncome.subtract(totalExpenses),
                            money(rs, "total_debt_remaining"),
                            money(rs, "total_debt_paid_in_month"),
                            count(rs, "active_debts_count"),
                            count(rs, "paid_debts_count"),
                            budgetExpected,
                            budgetPaid,
                            budgetExpected.subtract(budgetPaid),
                            Instant.now()
                    );
                },
                accountId, startInclusive, endInclusive,
                accountId, startInclusive, endInclusive,
                accountId,
                accountId, startInclusive, endInclusive,
                accountId,
                accountId,
                accountId, year, month,
                accountId, year, month
        );
    }

    @Override
    public CashflowSummaryResponse getCashflowSummary(CashflowSummaryQuery query) {
        MapSqlParameterSource params = rangeParams(query.accountId(), query.from(), query.to())
                .addValue("participantId", query.participantId())
                .addValue("categoryId", query.categoryId())
                .addValue("paymentMethodId", query.paymentMethodId());

        String sql = """
                SELECT
                    COALESCE((SELECT SUM(i.amount) FROM incomes i WHERE %s), 0) AS total_income,
                    COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE %s), 0) AS total_simple_expense_outflow,
                    COALESCE((
                        SELECT SUM(dp.amount)
                        FROM debt_payments dp
                        JOIN debts debt ON debt.account_id = dp.account_id AND debt.id = dp.debt_id
                        LEFT JOIN expenses origin ON origin.account_id = debt.account_id AND origin.id = debt.origin_expense_id
                        WHERE %s
                    ), 0) AS total_debt_payment_outflow
                """.formatted(
                incomeFilter("i", null, true, query.categoryId(), query.participantId()),
                cashSimpleExpenseFilter("e", query.categoryId(), query.paymentMethodId(), query.participantId()),
                debtPaymentFilter(query.categoryId(), query.paymentMethodId(), query.participantId())
        );

        return namedJdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            BigDecimal totalIncome = money(rs, "total_income");
            BigDecimal simpleOutflow = money(rs, "total_simple_expense_outflow");
            BigDecimal debtOutflow = money(rs, "total_debt_payment_outflow");
            BigDecimal totalOutflow = simpleOutflow.add(debtOutflow);
            return new CashflowSummaryResponse(
                    query.accountId(),
                    query.from(),
                    query.to(),
                    totalIncome,
                    simpleOutflow,
                    debtOutflow,
                    totalOutflow,
                    totalIncome.subtract(totalOutflow),
                    Instant.now()
            );
        });
    }

    @Override
    public ExpenseSummaryResponse getExpenseSummary(ExpenseSummaryQuery query) {
        MapSqlParameterSource params = expenseParams(query.accountId(), query.from(), query.to(), query.categoryId(), query.paymentMethodId(), query.participantId())
                .addValue("expenseType", enumName(query.expenseType()))
                .addValue("paymentState", enumName(query.paymentState()))
                .addValue("status", enumName(query.status()));

        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN e.expense_type = 'SIMPLE' THEN e.amount ELSE 0 END), 0) AS total_simple_expenses,
                    COALESCE(SUM(CASE WHEN e.expense_type = 'INSTALLMENT' THEN e.amount ELSE 0 END), 0) AS total_installment_purchases,
                    COALESCE(SUM(e.amount), 0) AS total_expenses_conceptual,
                    COALESCE(COUNT(e.id), 0) AS expenses_count
                FROM expenses e
                WHERE %s
                """.formatted(expenseFilter("e", query.status(), true, query.categoryId(), query.paymentMethodId(), query.participantId(), query.paymentState(), query.expenseType()));

        return namedJdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new ExpenseSummaryResponse(
                query.accountId(),
                query.from(),
                query.to(),
                money(rs, "total_simple_expenses"),
                money(rs, "total_installment_purchases"),
                money(rs, "total_expenses_conceptual"),
                count(rs, "expenses_count"),
                Instant.now()
        ));
    }

    @Override
    public List<CashflowItem> getCashflow(Long accountId, LocalDate from, LocalDate to, CashflowGroupBy groupBy, Long participantId) {
        MapSqlParameterSource params = rangeParams(accountId, from, to)
                .addValue("participantId", participantId);

        String incomeFilter = incomeFilter("i", null, true, null, participantId);
        String simpleExpenseFilter = cashSimpleExpenseFilter("e", null, null, participantId);
        String debtPaymentFilter = debtPaymentFilter(null, null, participantId);
        String incomePeriod = periodStartExpression("i.income_date", groupBy);
        String expensePeriod = periodStartExpression("e.expense_date", groupBy);
        String paymentPeriod = periodStartExpression("dp.payment_date", groupBy);
        String periodLabel = periodLabelExpression(groupBy);

        String sql = """
                SELECT
                    %s AS period,
                    COALESCE(SUM(total_income), 0) AS total_income,
                    COALESCE(SUM(simple_expense_outflow), 0) AS simple_expense_outflow,
                    COALESCE(SUM(debt_payment_outflow), 0) AS debt_payment_outflow
                FROM (
                    SELECT %s AS period_start, SUM(i.amount) AS total_income, 0::numeric AS simple_expense_outflow, 0::numeric AS debt_payment_outflow
                    FROM incomes i
                    WHERE %s
                    GROUP BY period_start
                    UNION ALL
                    SELECT %s AS period_start, 0::numeric AS total_income, SUM(e.amount) AS simple_expense_outflow, 0::numeric AS debt_payment_outflow
                    FROM expenses e
                    WHERE %s
                    GROUP BY period_start
                    UNION ALL
                    SELECT %s AS period_start, 0::numeric AS total_income, 0::numeric AS simple_expense_outflow, SUM(dp.amount) AS debt_payment_outflow
                    FROM debt_payments dp
                    JOIN debts debt ON debt.account_id = dp.account_id AND debt.id = dp.debt_id
                    LEFT JOIN expenses origin ON origin.account_id = debt.account_id AND origin.id = debt.origin_expense_id
                    WHERE %s
                    GROUP BY period_start
                ) cashflow
                GROUP BY period_start
                ORDER BY period_start ASC
                """.formatted(periodLabel, incomePeriod, incomeFilter, expensePeriod, simpleExpenseFilter, paymentPeriod, debtPaymentFilter);

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> cashflowItem(rs));
    }

    @Override
    public List<CategoryAmountItem> getExpensesByCategory(ExpenseBreakdownQuery query) {
        MapSqlParameterSource params = expenseParams(query.accountId(), query.from(), query.to(), query.categoryId(), query.paymentMethodId(), query.participantId())
                .addValue("paymentState", enumName(query.paymentState()))
                .addValue("expenseType", enumName(query.expenseType()))
                .addValue("status", enumName(query.status()));

        String sql = """
                SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(e.amount), 0) AS amount, COUNT(e.id) AS movement_count
                FROM expenses e
                JOIN categories c ON c.account_id = e.account_id AND c.id = e.category_id
                WHERE %s
                GROUP BY c.id, c.name
                ORDER BY amount DESC, c.name ASC
                """.formatted(expenseFilter("e", query.status(), true, query.categoryId(), query.paymentMethodId(), query.participantId(), query.paymentState(), query.expenseType()));

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> categoryItem(rs));
    }

    @Override
    public List<CategoryAmountItem> getIncomesByCategory(IncomeBreakdownQuery query) {
        MapSqlParameterSource params = rangeParams(query.accountId(), query.from(), query.to())
                .addValue("categoryId", query.categoryId())
                .addValue("participantId", query.participantId())
                .addValue("status", enumName(query.status()));

        String sql = """
                SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(i.amount), 0) AS amount, COUNT(i.id) AS movement_count
                FROM incomes i
                JOIN categories c ON c.account_id = i.account_id AND c.id = i.category_id
                WHERE %s
                GROUP BY c.id, c.name
                ORDER BY amount DESC, c.name ASC
                """.formatted(incomeFilter("i", query.status(), true, query.categoryId(), query.participantId()));

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> categoryItem(rs));
    }

    @Override
    public List<PaymentMethodAmountItem> getExpensesByPaymentMethod(ExpenseBreakdownQuery query) {
        MapSqlParameterSource params = expenseParams(query.accountId(), query.from(), query.to(), query.categoryId(), query.paymentMethodId(), query.participantId())
                .addValue("paymentState", enumName(query.paymentState()))
                .addValue("expenseType", enumName(query.expenseType()))
                .addValue("status", enumName(query.status()));

        String sql = """
                SELECT pm.id AS payment_method_id, pm.name AS payment_method_name, COALESCE(SUM(e.amount), 0) AS amount, COUNT(e.id) AS movement_count
                FROM expenses e
                JOIN payment_methods pm ON pm.account_id = e.account_id AND pm.id = e.payment_method_id
                WHERE %s
                GROUP BY pm.id, pm.name
                ORDER BY amount DESC, pm.name ASC
                """.formatted(expenseFilter("e", query.status(), true, query.categoryId(), query.paymentMethodId(), query.participantId(), query.paymentState(), query.expenseType()));

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> paymentMethodItem(rs));
    }

    @Override
    public DebtSummaryResponse getDebtSummary(Long accountId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    COALESCE(SUM(CASE WHEN d.state = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active_debts_count,
                    COALESCE(SUM(CASE WHEN d.state = 'PAID' THEN 1 ELSE 0 END), 0) AS paid_debts_count,
                    COALESCE(SUM(CASE WHEN d.state = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_debts_count,
                    COALESCE(SUM(CASE WHEN d.state <> 'CANCELLED' THEN d.total_amount ELSE 0 END), 0) AS total_debt_amount,
                    COALESCE(SUM(CASE WHEN d.state = 'ACTIVE' THEN d.remaining_amount ELSE 0 END), 0) AS total_remaining_balance,
                    COALESCE((
                        SELECT SUM(dp.amount)
                        FROM debt_payments dp
                        JOIN debts paid_debt ON paid_debt.account_id = dp.account_id AND paid_debt.id = dp.debt_id
                        WHERE dp.account_id = ?
                          AND dp.status = 'ACTIVE'
                          AND paid_debt.state <> 'CANCELLED'
                    ), 0) AS total_paid_amount,
                    COALESCE(SUM(CASE WHEN d.state <> 'CANCELLED' AND d.source_type = 'MANUAL' THEN 1 ELSE 0 END), 0) AS manual_debts_count,
                    COALESCE(SUM(CASE WHEN d.state <> 'CANCELLED' AND d.source_type = 'INSTALLMENT_EXPENSE' THEN 1 ELSE 0 END), 0) AS installment_expense_debts_count
                FROM debts d
                WHERE d.account_id = ?
                """,
                (rs, rowNum) -> new DebtSummaryResponse(
                        accountId,
                        count(rs, "active_debts_count"),
                        count(rs, "paid_debts_count"),
                        count(rs, "cancelled_debts_count"),
                        money(rs, "total_debt_amount"),
                        money(rs, "total_remaining_balance"),
                        money(rs, "total_paid_amount"),
                        count(rs, "manual_debts_count"),
                        count(rs, "installment_expense_debts_count")
                ),
                accountId,
                accountId
        );
    }

    @Override
    public BudgetSummaryResponse getBudgetSummary(Long accountId, Integer year, Integer month) {
        YearMonth period = YearMonth.of(year, month);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("year", year)
                .addValue("month", month)
                .addValue("from", period.atDay(1))
                .addValue("to", period.atEndOfMonth());
        return namedJdbcTemplate.query(
                """
                WITH budget_row AS (
                    SELECT b.id
                    FROM budgets b
                    WHERE b.account_id = :accountId
                      AND b.year = :year
                      AND b.month = :month
                ),
                manual_categories AS (
                    SELECT DISTINCT sb.category_id
                    FROM sub_budgets sb
                    JOIN budget_row b ON b.id = sb.budget_id
                    WHERE sb.account_id = :accountId
                      AND sb.status = 'ACTIVE'
                      AND sb.source_type = 'MANUAL'
                      AND sb.category_id IS NOT NULL
                ),
                manual_budget AS (
                    SELECT COALESCE(SUM(sb.planned_amount), 0) AS expected_amount
                    FROM sub_budgets sb
                    JOIN budget_row b ON b.id = sb.budget_id
                    WHERE sb.account_id = :accountId
                      AND sb.status = 'ACTIVE'
                      AND sb.source_type = 'MANUAL'
                ),
                manual_execution AS (
                    SELECT COALESCE(SUM(e.amount), 0) AS paid_amount
                    FROM expenses e
                    WHERE e.account_id = :accountId
                      AND e.expense_date BETWEEN :from AND :to
                      AND e.status = 'ACTIVE'
                      AND e.expense_type = 'SIMPLE'
                      AND e.source_type IN ('MANUAL', 'IMPORT')
                      AND EXISTS (
                          SELECT 1
                          FROM manual_categories mc
                          WHERE mc.category_id = e.category_id
                      )
                ),
                impact_budget AS (
                    SELECT
                        COALESCE(SUM(CASE WHEN bi.status IN ('ACTIVE', 'PAID') THEN bi.expected_amount ELSE 0 END), 0) AS expected_amount,
                        COALESCE(SUM(CASE WHEN bi.status IN ('ACTIVE', 'PAID') THEN bi.paid_amount ELSE 0 END), 0) AS paid_amount,
                        COALESCE(COUNT(bi.id), 0) AS impacts_count,
                        COALESCE(SUM(CASE WHEN bi.status = 'PAID' THEN 1 ELSE 0 END), 0) AS paid_impacts_count,
                        COALESCE(SUM(CASE WHEN bi.status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active_impacts_count
                    FROM budget_impacts bi
                    JOIN budget_row b ON b.id = bi.budget_id
                    WHERE bi.account_id = :accountId
                ),
                sub_budget_count AS (
                    SELECT COALESCE(COUNT(sb.id), 0) AS sub_budgets_count
                    FROM sub_budgets sb
                    JOIN budget_row b ON b.id = sb.budget_id
                    WHERE sb.account_id = :accountId
                )
                SELECT
                    b.id AS budget_id,
                    COALESCE(mb.expected_amount, 0) + COALESCE(ib.expected_amount, 0) AS expected_amount,
                    COALESCE(me.paid_amount, 0) + COALESCE(ib.paid_amount, 0) AS paid_amount,
                    COALESCE(ib.impacts_count, 0) AS impacts_count,
                    COALESCE(ib.paid_impacts_count, 0) AS paid_impacts_count,
                    COALESCE(ib.active_impacts_count, 0) AS active_impacts_count,
                    COALESCE(sbc.sub_budgets_count, 0) AS sub_budgets_count
                FROM budget_row b
                CROSS JOIN manual_budget mb
                CROSS JOIN manual_execution me
                CROSS JOIN impact_budget ib
                CROSS JOIN sub_budget_count sbc
                """,
                params,
                rs -> {
                    if (!rs.next()) {
                        return zeroBudgetSummary(accountId, year, month);
                    }
                    BigDecimal expected = money(rs, "expected_amount");
                    BigDecimal paid = money(rs, "paid_amount");
                    return new BudgetSummaryResponse(
                            accountId,
                            year,
                            month,
                            rs.getLong("budget_id"),
                            expected,
                            paid,
                            expected.subtract(paid),
                            count(rs, "impacts_count"),
                            count(rs, "paid_impacts_count"),
                            count(rs, "active_impacts_count"),
                            count(rs, "sub_budgets_count")
                    );
                }
        );
    }

    @Override
    public List<BudgetVsExpensesCategoryItem> getBudgetVsExpensesByCategory(Long accountId, Integer year, Integer month, LocalDate from, LocalDate to) {
        MapSqlParameterSource params = rangeParams(accountId, from, to)
                .addValue("year", year)
                .addValue("month", month);

        String budgetSql = """
                SELECT
                    c.id AS category_id,
                    c.name AS category_name,
                    COALESCE(SUM(sb.planned_amount), 0) AS amount
                FROM budgets b
                JOIN sub_budgets sb ON sb.account_id = b.account_id AND sb.budget_id = b.id
                JOIN categories c ON c.account_id = sb.account_id AND c.id = sb.category_id
                WHERE b.account_id = :accountId
                  AND b.year = :year
                  AND b.month = :month
                  AND sb.status = 'ACTIVE'
                  AND sb.source_type = 'MANUAL'
                  AND sb.category_id IS NOT NULL
                GROUP BY c.id, c.name
                """;

        String spentSql = """
                SELECT c.id AS category_id, c.name AS category_name, COALESCE(SUM(e.amount), 0) AS amount
                FROM expenses e
                JOIN categories c ON c.account_id = e.account_id AND c.id = e.category_id
                WHERE %s
                GROUP BY c.id, c.name
                """.formatted(expenseFilter("e", null, true, null, null, null, null, null));

        Map<Long, BudgetVsExpensesAccumulator> items = new LinkedHashMap<>();
        namedJdbcTemplate.query(budgetSql, params, rs -> {
            BudgetVsExpensesAccumulator item = accumulator(items, rs);
            item.budgetedAmount = money(rs, "amount");
        });
        namedJdbcTemplate.query(spentSql, params, rs -> {
            BudgetVsExpensesAccumulator item = accumulator(items, rs);
            item.spentAmount = money(rs, "amount");
        });

        return items.values().stream()
                .map(BudgetVsExpensesAccumulator::toItem)
                .sorted(Comparator.comparing(BudgetVsExpensesCategoryItem::spentAmount).reversed()
                        .thenComparing(BudgetVsExpensesCategoryItem::categoryName))
                .toList();
    }

    private static BudgetSummaryResponse zeroBudgetSummary(Long accountId, Integer year, Integer month) {
        return new BudgetSummaryResponse(accountId, year, month, null, ZERO, ZERO, ZERO, 0L, 0L, 0L, 0L);
    }

    private static MapSqlParameterSource rangeParams(Long accountId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("from", from)
                .addValue("to", to);
    }

    private static MapSqlParameterSource expenseParams(
            Long accountId,
            LocalDate from,
            LocalDate to,
            Long categoryId,
            Long paymentMethodId,
            Long participantId
    ) {
        return rangeParams(accountId, from, to)
                .addValue("categoryId", categoryId)
                .addValue("paymentMethodId", paymentMethodId)
                .addValue("participantId", participantId);
    }

    private static String incomeFilter(String alias, Enum<?> status, boolean defaultActive, Long categoryId, Long participantId) {
        StringBuilder filter = new StringBuilder(alias)
                .append(".account_id = :accountId AND ")
                .append(alias)
                .append(".income_date BETWEEN :from AND :to");
        if (status != null) {
            filter.append(" AND ").append(alias).append(".status = :status");
        } else if (defaultActive) {
            filter.append(" AND ").append(alias).append(".status = 'ACTIVE'");
        }
        if (categoryId != null) {
            filter.append(" AND ").append(alias).append(".category_id = :categoryId");
        }
        if (participantId != null) {
            filter.append(" AND ").append(alias).append(".participant_id = :participantId");
        }
        return filter.toString();
    }

    private static String cashSimpleExpenseFilter(String alias, Long categoryId, Long paymentMethodId, Long participantId) {
        StringBuilder filter = new StringBuilder(alias)
                .append(".account_id = :accountId AND ")
                .append(alias)
                .append(".expense_date BETWEEN :from AND :to")
                .append(" AND ").append(alias).append(".status = 'ACTIVE'")
                .append(" AND ").append(alias).append(".expense_type = 'SIMPLE'")
                .append(" AND ").append(alias).append(".payment_state = 'PAID'")
                .append(" AND ").append(alias).append(".source_type <> 'DEBT_PAYMENT'");
        if (categoryId != null) {
            filter.append(" AND ").append(alias).append(".category_id = :categoryId");
        }
        if (paymentMethodId != null) {
            filter.append(" AND ").append(alias).append(".payment_method_id = :paymentMethodId");
        }
        if (participantId != null) {
            filter.append(" AND ").append(alias).append(".participant_id = :participantId");
        }
        return filter.toString();
    }

    private static String debtPaymentFilter(Long categoryId, Long paymentMethodId, Long participantId) {
        StringBuilder filter = new StringBuilder()
                .append("dp.account_id = :accountId")
                .append(" AND dp.status = 'ACTIVE'")
                .append(" AND dp.payment_date BETWEEN :from AND :to")
                .append(" AND debt.state <> 'CANCELLED'");
        if (participantId != null) {
            filter.append(" AND dp.participant_id = :participantId");
        }
        if (categoryId != null) {
            filter.append(" AND origin.category_id = :categoryId");
        }
        if (paymentMethodId != null) {
            filter.append(" AND origin.payment_method_id = :paymentMethodId");
        }
        return filter.toString();
    }

    private static String expenseFilter(
            String alias,
            Enum<?> status,
            boolean defaultActive,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            Enum<?> paymentState,
            Enum<?> expenseType
    ) {
        StringBuilder filter = new StringBuilder(alias)
                .append(".account_id = :accountId AND ")
                .append(alias)
                .append(".expense_date BETWEEN :from AND :to");
        if (status != null) {
            filter.append(" AND ").append(alias).append(".status = :status");
        } else if (defaultActive) {
            filter.append(" AND ").append(alias).append(".status = 'ACTIVE'");
        }
        if (categoryId != null) {
            filter.append(" AND ").append(alias).append(".category_id = :categoryId");
        }
        if (paymentMethodId != null) {
            filter.append(" AND ").append(alias).append(".payment_method_id = :paymentMethodId");
        }
        if (participantId != null) {
            filter.append(" AND ").append(alias).append(".participant_id = :participantId");
        }
        if (paymentState != null) {
            filter.append(" AND ").append(alias).append(".payment_state = :paymentState");
        }
        if (expenseType != null) {
            filter.append(" AND ").append(alias).append(".expense_type = :expenseType");
        }
        return filter.toString();
    }

    private static String periodStartExpression(String column, CashflowGroupBy groupBy) {
        return switch (groupBy) {
            case DAY -> column;
            case WEEK -> "date_trunc('week', " + column + ")::date";
            case MONTH -> "date_trunc('month', " + column + ")::date";
        };
    }

    private static String periodLabelExpression(CashflowGroupBy groupBy) {
        return switch (groupBy) {
            case DAY, WEEK -> "to_char(period_start, 'YYYY-MM-DD')";
            case MONTH -> "to_char(period_start, 'YYYY-MM')";
        };
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static CategoryAmountItem categoryItem(ResultSet rs) throws SQLException {
        return new CategoryAmountItem(
                rs.getLong("category_id"),
                rs.getString("category_name"),
                money(rs, "amount"),
                count(rs, "movement_count")
        );
    }

    private static PaymentMethodAmountItem paymentMethodItem(ResultSet rs) throws SQLException {
        return new PaymentMethodAmountItem(
                rs.getLong("payment_method_id"),
                rs.getString("payment_method_name"),
                money(rs, "amount"),
                count(rs, "movement_count")
        );
    }

    private static CashflowItem cashflowItem(ResultSet rs) throws SQLException {
        BigDecimal totalIncome = money(rs, "total_income");
        BigDecimal simpleOutflow = money(rs, "simple_expense_outflow");
        BigDecimal debtOutflow = money(rs, "debt_payment_outflow");
        BigDecimal totalOutflow = simpleOutflow.add(debtOutflow);
        return new CashflowItem(
                rs.getString("period"),
                totalIncome,
                simpleOutflow,
                debtOutflow,
                totalOutflow,
                totalIncome.subtract(totalOutflow)
        );
    }

    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? ZERO : value.setScale(2);
    }

    private static Long count(ResultSet rs, String column) throws SQLException {
        return rs.getLong(column);
    }

    private static BudgetVsExpensesAccumulator accumulator(Map<Long, BudgetVsExpensesAccumulator> items, ResultSet rs) throws SQLException {
        Long categoryId = rs.getLong("category_id");
        BudgetVsExpensesAccumulator item = items.get(categoryId);
        if (item == null) {
            item = new BudgetVsExpensesAccumulator(categoryId, rs.getString("category_name"));
            items.put(categoryId, item);
        }
        return item;
    }

    private static final class BudgetVsExpensesAccumulator {
        private final Long categoryId;
        private final String categoryName;
        private BigDecimal budgetedAmount = ZERO;
        private BigDecimal spentAmount = ZERO;

        private BudgetVsExpensesAccumulator(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        private BudgetVsExpensesCategoryItem toItem() {
            return new BudgetVsExpensesCategoryItem(
                    categoryId,
                    categoryName,
                    budgetedAmount,
                    spentAmount,
                    budgetedAmount.subtract(spentAmount),
                    executionPercentage()
            );
        }

        private BigDecimal executionPercentage() {
            if (budgetedAmount.signum() > 0) {
                return spentAmount.multiply(new BigDecimal("100")).divide(budgetedAmount, 2, RoundingMode.HALF_UP);
            }
            if (spentAmount.signum() > 0) {
                return null;
            }
            return ZERO;
        }
    }
}
