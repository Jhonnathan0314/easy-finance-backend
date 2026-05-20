package com.easyfinance.imports.infrastructure.mapper;

import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseImportPersistenceMapperTest {

    private final ExpenseImportPersistenceMapper mapper = new ExpenseImportPersistenceMapper();

    @Test
    void mapsDebtPaymentContractFieldsToRowEntity() {
        ExpenseImportRow row = new ExpenseImportRow(
                101L,
                1L,
                77L,
                2,
                LocalDate.of(2026, 5, 1),
                "Debt payment lunch",
                Money.cop(new BigDecimal("120.00")),
                "Food",
                10L,
                "Cash",
                20L,
                ExpensePaymentState.PAID,
                true,
                30L,
                "Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL",
                DebtPaymentType.INSTALLMENT,
                "Installment from import",
                true,
                List.of(),
                500L,
                600L,
                null,
                null
        );

        var entity = mapper.toRowEntity(row, 77L);

        assertThat(entity.isAppliesDebtPayment()).isTrue();
        assertThat(entity.getDebtId()).isEqualTo(30L);
        assertThat(entity.getDebtLabel()).isEqualTo("Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL");
        assertThat(entity.getDebtPaymentType()).isEqualTo("INSTALLMENT");
        assertThat(entity.getDebtPaymentNotes()).isEqualTo("Installment from import");
        assertThat(entity.getCreatedDebtPaymentId()).isEqualTo(600L);
    }

    @Test
    void mapsDebtPaymentContractFieldsToDomainRow() {
        var entity = mapper.toRowEntity(new ExpenseImportRow(
                101L,
                1L,
                77L,
                2,
                LocalDate.of(2026, 5, 1),
                "Debt payment lunch",
                Money.cop(new BigDecimal("120.00")),
                "Food",
                10L,
                "Cash",
                20L,
                ExpensePaymentState.PAID,
                true,
                30L,
                "Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL",
                DebtPaymentType.CAPITAL_PAYMENT,
                "Capital from import",
                true,
                List.of(),
                500L,
                600L,
                null,
                null
        ), 77L);

        ExpenseImportRow row = mapper.toRowDomain(entity);

        assertThat(row.appliesDebtPayment()).isTrue();
        assertThat(row.debtId()).isEqualTo(30L);
        assertThat(row.debtLabel()).isEqualTo("Loan | Saldo: 120.00 | Inicio: 2026-05-01 | MANUAL");
        assertThat(row.debtPaymentType()).isEqualTo(DebtPaymentType.CAPITAL_PAYMENT);
        assertThat(row.debtPaymentNotes()).isEqualTo("Capital from import");
        assertThat(row.createdDebtPaymentId()).isEqualTo(600L);
    }
}
