package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;

public interface BudgetDebtImpactPort {
    void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command);

    void applyDebtPaymentToImpacts(ApplyDebtPaymentImpactCommand command);

    void cancelActiveImpactsForDebt(Long accountId, Long debtId);
}
