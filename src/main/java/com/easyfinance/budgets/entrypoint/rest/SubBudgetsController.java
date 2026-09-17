package com.easyfinance.budgets.entrypoint.rest;

import com.easyfinance.budgets.application.port.in.CreateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DeactivateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.SubBudgetForwardPort;
import com.easyfinance.budgets.application.port.in.UpdateSubBudgetPort;
import com.easyfinance.budgets.entrypoint.rest.dto.CreateSubBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetForwardApplyRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetForwardApplyResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetForwardPlanResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetForwardRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.SubBudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.UpdateSubBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.mapper.BudgetRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets")
public class SubBudgetsController {

    private final CreateSubBudgetPort createSubBudgetPort;
    private final UpdateSubBudgetPort updateSubBudgetPort;
    private final DeactivateSubBudgetPort deactivateSubBudgetPort;
    private final SubBudgetForwardPort subBudgetForwardPort;

    public SubBudgetsController(
            CreateSubBudgetPort createSubBudgetPort,
            UpdateSubBudgetPort updateSubBudgetPort,
            DeactivateSubBudgetPort deactivateSubBudgetPort,
            SubBudgetForwardPort subBudgetForwardPort
    ) {
        this.createSubBudgetPort = createSubBudgetPort;
        this.updateSubBudgetPort = updateSubBudgetPort;
        this.deactivateSubBudgetPort = deactivateSubBudgetPort;
        this.subBudgetForwardPort = subBudgetForwardPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubBudgetResponseDto create(
            @PathVariable Long accountId,
            @PathVariable Long budgetId,
            @Valid @RequestBody CreateSubBudgetRequest request
    ) {
        return BudgetRestMapper.toDto(createSubBudgetPort.createSubBudget(BudgetRestMapper.toCommand(accountId, budgetId, request)));
    }

    @PutMapping("/{subBudgetId}")
    public SubBudgetResponseDto update(
            @PathVariable Long accountId,
            @PathVariable Long budgetId,
            @PathVariable Long subBudgetId,
            @Valid @RequestBody UpdateSubBudgetRequest request
    ) {
        return BudgetRestMapper.toDto(updateSubBudgetPort.updateSubBudget(BudgetRestMapper.toCommand(accountId, budgetId, subBudgetId, request)));
    }

    @DeleteMapping("/{subBudgetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long accountId, @PathVariable Long budgetId, @PathVariable Long subBudgetId) {
        deactivateSubBudgetPort.deactivateSubBudget(accountId, budgetId, subBudgetId);
    }

    @PostMapping("/forward-preview")
    public SubBudgetForwardPlanResponseDto previewForward(
            @PathVariable Long accountId,
            @PathVariable Long budgetId,
            @Valid @RequestBody SubBudgetForwardRequest request
    ) {
        return BudgetRestMapper.toDto(subBudgetForwardPort.previewSubBudgetForward(BudgetRestMapper.toPreviewCommand(accountId, budgetId, request)));
    }

    @PostMapping("/forward-apply")
    public SubBudgetForwardApplyResponseDto applyForward(
            @PathVariable Long accountId,
            @PathVariable Long budgetId,
            @Valid @RequestBody SubBudgetForwardApplyRequest request
    ) {
        return BudgetRestMapper.toDto(subBudgetForwardPort.applySubBudgetForward(BudgetRestMapper.toApplyCommand(accountId, budgetId, request)));
    }
}
