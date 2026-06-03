package com.easyfinance.imports.application.validation;

import java.util.List;

public sealed interface AnnualBudgetImportMonthScope permits AnnualBudgetImportMonthScope.AllMonths, AnnualBudgetImportMonthScope.SingleMonth {

    List<Integer> months();

    record AllMonths() implements AnnualBudgetImportMonthScope {
        @Override
        public List<Integer> months() {
            return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        }
    }

    record SingleMonth(int month) implements AnnualBudgetImportMonthScope {
        public SingleMonth {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be between 1 and 12.");
            }
        }

        @Override
        public List<Integer> months() {
            return List.of(month);
        }
    }
}

