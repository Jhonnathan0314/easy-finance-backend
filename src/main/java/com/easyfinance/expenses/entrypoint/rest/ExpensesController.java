package com.easyfinance.expenses.entrypoint.rest;

import com.easyfinance.expenses.application.port.in.CancelExpensePort;
import com.easyfinance.expenses.application.port.in.CreateExpensePort;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.expenses.application.port.in.DuplicateExpensePort;
import com.easyfinance.expenses.application.port.in.GetExpensePort;
import com.easyfinance.expenses.application.port.in.ListExpensesPort;
import com.easyfinance.expenses.application.port.in.UpdateExpensePort;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.expenses.entrypoint.rest.dto.CreateExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.CreateInstallmentExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.DuplicateExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.ExpensePaymentStateDto;
import com.easyfinance.expenses.entrypoint.rest.dto.ExpenseResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.ExpenseStatusDto;
import com.easyfinance.expenses.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.UpdateExpenseRequest;
import com.easyfinance.expenses.entrypoint.rest.mapper.ExpenseRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/expenses")
public class ExpensesController {

    private final CreateExpensePort createExpensePort;
    private final CreateInstallmentExpensePort createInstallmentExpensePort;
    private final ListExpensesPort listExpensesPort;
    private final GetExpensePort getExpensePort;
    private final UpdateExpensePort updateExpensePort;
    private final CancelExpensePort cancelExpensePort;
    private final DuplicateExpensePort duplicateExpensePort;

    public ExpensesController(
            CreateExpensePort createExpensePort,
            CreateInstallmentExpensePort createInstallmentExpensePort,
            ListExpensesPort listExpensesPort,
            GetExpensePort getExpensePort,
            UpdateExpensePort updateExpensePort,
            CancelExpensePort cancelExpensePort,
            DuplicateExpensePort duplicateExpensePort
    ) {
        this.createExpensePort = createExpensePort;
        this.createInstallmentExpensePort = createInstallmentExpensePort;
        this.listExpensesPort = listExpensesPort;
        this.getExpensePort = getExpensePort;
        this.updateExpensePort = updateExpensePort;
        this.cancelExpensePort = cancelExpensePort;
        this.duplicateExpensePort = duplicateExpensePort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDto create(@PathVariable Long accountId, @Valid @RequestBody CreateExpenseRequest request) {
        return ExpenseRestMapper.toDto(createExpensePort.createExpense(ExpenseRestMapper.toCommand(accountId, request)));
    }

    @PostMapping("/installments")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDto createInstallment(@PathVariable Long accountId, @Valid @RequestBody CreateInstallmentExpenseRequest request) {
        return ExpenseRestMapper.toDto(createInstallmentExpensePort.createInstallmentExpense(ExpenseRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<ExpenseResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) ExpensePaymentStateDto paymentState,
            @RequestParam(required = false) ExpenseStatusDto status,
            @RequestParam(required = false) ExpenseType expenseType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListExpensesQuery(
                accountId,
                from,
                to,
                categoryId,
                paymentMethodId,
                participantId,
                paymentState == null ? null : ExpensePaymentState.valueOf(paymentState.name()),
                status == null ? null : ExpenseStatus.valueOf(status.name()),
                expenseType,
                search,
                PageQuery.of(page, size),
                sort
        );
        return ExpenseRestMapper.toDto(listExpensesPort.listExpenses(query), ExpenseRestMapper::toDto);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponseDto get(@PathVariable Long accountId, @PathVariable Long expenseId) {
        return ExpenseRestMapper.toDto(getExpensePort.getExpense(accountId, expenseId));
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponseDto update(
            @PathVariable Long accountId,
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        return ExpenseRestMapper.toDto(updateExpensePort.updateExpense(ExpenseRestMapper.toCommand(accountId, expenseId, request)));
    }

    @PatchMapping("/{expenseId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long accountId, @PathVariable Long expenseId) {
        cancelExpensePort.cancelExpense(accountId, expenseId);
    }

    @PostMapping("/{expenseId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDto duplicate(
            @PathVariable Long accountId,
            @PathVariable Long expenseId,
            @Valid @RequestBody DuplicateExpenseRequest request
    ) {
        return ExpenseRestMapper.toDto(duplicateExpensePort.duplicateExpense(ExpenseRestMapper.toCommand(accountId, expenseId, request)));
    }
}
