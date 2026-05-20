package com.easyfinance.analytics.infrastructure.persistence;

import com.easyfinance.analytics.infrastructure.persistence.jpa.JpaAnalyticsQueryAdapter;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.income.domain.model.IncomeStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAnalyticsQueryAdapterSqlFilterTest {

    @Test
    void incomeFilterWithoutExplicitStatusUsesActiveLiteralWithoutNullSensitiveParameters() throws Exception {
        String sql = invokeIncomeFilter(null);

        assertThat(sql).contains("i.status = 'ACTIVE'");
        assertThat(sql).doesNotContain(":status IS");
        assertThat(sql).doesNotContain(" OR ");
    }

    @Test
    void incomeFilterWithExplicitStatusUsesOnlyStatusComparison() throws Exception {
        String sql = invokeIncomeFilter(IncomeStatus.CANCELLED);

        assertThat(sql).contains("i.status = :status");
        assertThat(sql).doesNotContain("i.status = 'ACTIVE'");
        assertThat(sql).doesNotContain(":status IS");
        assertThat(sql).doesNotContain(" OR ");
    }

    @Test
    void expenseFilterWithoutOptionalValuesUsesActiveLiteralWithoutNullSensitiveParameters() throws Exception {
        Method method = JpaAnalyticsQueryAdapter.class.getDeclaredMethod(
                "expenseFilter",
                String.class,
                Enum.class,
                boolean.class,
                Long.class,
                Long.class,
                Long.class,
                Enum.class,
                Enum.class
        );
        method.setAccessible(true);

        String sql = (String) method.invoke(null, "e", null, true, null, null, null, null, null);

        assertThat(sql).contains("e.status = 'ACTIVE'");
        assertThat(sql).doesNotContain(" IS NULL");
        assertThat(sql).doesNotContain("IS NOT NULL");
        assertThat(sql).doesNotContain(" OR ");
    }

    @Test
    void expenseFilterWithExplicitStatusUsesOnlyStatusComparison() throws Exception {
        Method method = JpaAnalyticsQueryAdapter.class.getDeclaredMethod(
                "expenseFilter",
                String.class,
                Enum.class,
                boolean.class,
                Long.class,
                Long.class,
                Long.class,
                Enum.class,
                Enum.class
        );
        method.setAccessible(true);

        String sql = (String) method.invoke(null, "e", ExpenseStatus.CANCELLED, true, null, null, null, null, null);

        assertThat(sql).contains("e.status = :status");
        assertThat(sql).doesNotContain("e.status = 'ACTIVE'");
        assertThat(sql).doesNotContain(" IS NULL");
        assertThat(sql).doesNotContain("IS NOT NULL");
        assertThat(sql).doesNotContain(" OR ");
    }

    private static String invokeIncomeFilter(IncomeStatus status) throws Exception {
        Method method = JpaAnalyticsQueryAdapter.class.getDeclaredMethod(
                "incomeFilter",
                String.class,
                Enum.class,
                boolean.class,
                Long.class,
                Long.class
        );
        method.setAccessible(true);
        return (String) method.invoke(null, "i", status, true, null, null);
    }
}
