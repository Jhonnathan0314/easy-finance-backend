package com.easyfinance.budgets.entrypoint.rest;

import com.easyfinance.budgets.application.port.in.DuplicateBudgetPort;
import com.easyfinance.budgets.application.port.in.CreateAnnualBudgetPort;
import com.easyfinance.budgets.application.port.in.GetBudgetPort;
import com.easyfinance.budgets.application.port.in.ListBudgetsPort;
import com.easyfinance.budgets.application.port.in.UpsertBudgetPort;
import com.easyfinance.budgets.application.query.ListBudgetsQuery;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetDetailResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.BudgetStatusDto;
import com.easyfinance.budgets.entrypoint.rest.dto.AnnualBudgetResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.CreateAnnualBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.DuplicateBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.budgets.entrypoint.rest.dto.UpsertBudgetRequest;
import com.easyfinance.budgets.entrypoint.rest.mapper.BudgetRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/budgets")
public class BudgetsController {

    private final UpsertBudgetPort upsertBudgetPort;
    private final GetBudgetPort getBudgetPort;
    private final ListBudgetsPort listBudgetsPort;
    private final DuplicateBudgetPort duplicateBudgetPort;
    private final CreateAnnualBudgetPort createAnnualBudgetPort;

    public BudgetsController(UpsertBudgetPort upsertBudgetPort, GetBudgetPort getBudgetPort, ListBudgetsPort listBudgetsPort, DuplicateBudgetPort duplicateBudgetPort, CreateAnnualBudgetPort createAnnualBudgetPort) {
        this.upsertBudgetPort = upsertBudgetPort;
        this.getBudgetPort = getBudgetPort;
        this.listBudgetsPort = listBudgetsPort;
        this.duplicateBudgetPort = duplicateBudgetPort;
        this.createAnnualBudgetPort = createAnnualBudgetPort;
    }

    @PutMapping("/{year}/{month}")
    public BudgetResponseDto upsert(
            @PathVariable Long accountId,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @Valid @RequestBody(required = false) UpsertBudgetRequest request
    ) {
        return BudgetRestMapper.toDto(upsertBudgetPort.upsertBudget(BudgetRestMapper.toCommand(accountId, year, month, request)));
    }

    @GetMapping("/{year}/{month}")
    public BudgetDetailResponseDto get(@PathVariable Long accountId, @PathVariable Integer year, @PathVariable Integer month) {
        return BudgetRestMapper.toDto(getBudgetPort.getBudget(accountId, year, month));
    }

    @PostMapping("/{sourceYear}/{sourceMonth}/duplicate")
    public BudgetDetailResponseDto duplicate(
            @PathVariable Long accountId,
            @PathVariable Integer sourceYear,
            @PathVariable Integer sourceMonth,
            @Valid @RequestBody DuplicateBudgetRequest request
    ) {
        return BudgetRestMapper.toDto(duplicateBudgetPort.duplicateBudget(BudgetRestMapper.toCommand(accountId, sourceYear, sourceMonth, request)));
    }

    @PostMapping("/annual")
    public AnnualBudgetResponseDto createAnnual(
            @PathVariable Long accountId,
            @Valid @RequestBody CreateAnnualBudgetRequest request
    ) {
        return BudgetRestMapper.toDto(createAnnualBudgetPort.createAnnualBudget(BudgetRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<BudgetResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) BudgetStatusDto status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        var query = new ListBudgetsQuery(accountId, year, status == null ? null : BudgetStatus.valueOf(status.name()), sort, PageQuery.of(page, size));
        return BudgetRestMapper.toDto(listBudgetsPort.listBudgets(query), BudgetRestMapper::toDto);
    }
}
