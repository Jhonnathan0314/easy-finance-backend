package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.command.ApplySubBudgetForwardCommand;
import com.easyfinance.budgets.application.command.PreviewSubBudgetForwardCommand;
import com.easyfinance.budgets.application.response.SubBudgetForwardApplyResponse;
import com.easyfinance.budgets.application.response.SubBudgetForwardPlanResponse;

public interface SubBudgetForwardPort {
    SubBudgetForwardPlanResponse previewSubBudgetForward(PreviewSubBudgetForwardCommand command);

    SubBudgetForwardApplyResponse applySubBudgetForward(ApplySubBudgetForwardCommand command);
}
