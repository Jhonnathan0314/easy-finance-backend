package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.shared.domain.Money;

public interface BudgetDebtImpactPort {
    void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command);

    void applyDebtPaymentToImpacts(ApplyDebtPaymentImpactCommand command);

    void revertDebtPaymentFromImpacts(Long accountId, Long debtId, Money amount);

    void cancelActiveImpactsForDebt(Long accountId, Long debtId);
}
