package com.easyfinance.imports.domain.model;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseImportBatchTest {

    @Test
    void previewCalculatesTotalsFromRows() {
        ExpenseImportBatch batch = ExpenseImportBatch.preview(1L, 10L, "expenses.xlsx", List.of(validRow(), invalidRow()));

        assertThat(batch.status()).isEqualTo(ExpenseImportStatus.PREVIEW);
        assertThat(batch.totalRows()).isEqualTo(2);
        assertThat(batch.validRows()).isEqualTo(1);
        assertThat(batch.invalidRows()).isEqualTo(1);
    }

    @Test
    void confirmChangesStatusAndTimestamp() {
        ExpenseImportBatch batch = ExpenseImportBatch.preview(1L, 10L, "expenses.xlsx", List.of(validRow()));
        Instant confirmedAt = Instant.parse("2026-05-01T10:15:30Z");

        ExpenseImportBatch confirmed = batch.confirm(confirmedAt);

        assertThat(confirmed.status()).isEqualTo(ExpenseImportStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isEqualTo(confirmedAt);
    }

    @Test
    void confirmFailsWhenBatchHasNoValidRows() {
        ExpenseImportBatch batch = ExpenseImportBatch.preview(1L, 10L, "expenses.xlsx", List.of(invalidRow()));

        assertThatThrownBy(() -> batch.confirm(Instant.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("IMPORT_NO_VALID_ROWS"));
    }

    @Test
    void confirmedBatchCannotBeConfirmedAgain() {
        ExpenseImportBatch batch = ExpenseImportBatch.preview(1L, 10L, "expenses.xlsx", List.of(validRow()))
                .confirm(Instant.now());

        assertThatThrownBy(() -> batch.confirm(Instant.now()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("IMPORT_ALREADY_CONFIRMED"));
    }

    private static ExpenseImportRow validRow() {
        return new ExpenseImportRow(null, 1L, null, 2, LocalDate.of(2026, 5, 1), "Lunch",
                Money.cop(new BigDecimal("120.00")), "Food", 10L, "Cash", 20L,
                ExpensePaymentState.PAID, false, null, null, null, null,
                true, List.of(), null, null, null, null);
    }

    private static ExpenseImportRow invalidRow() {
        return new ExpenseImportRow(null, 1L, null, 3, null, null, null,
                null, null, null, null, null, false, null, null, null, null,
                false, List.of(new ImportRowError("Fecha", "REQUIRED", "Fecha is required.")), null, null, null, null);
    }
}
