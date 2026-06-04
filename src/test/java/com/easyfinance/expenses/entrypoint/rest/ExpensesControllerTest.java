package com.easyfinance.expenses.entrypoint.rest;

import com.easyfinance.expenses.application.port.in.CancelExpensePort;
import com.easyfinance.expenses.application.port.in.CreateExpensePort;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.expenses.application.port.in.DuplicateExpensePort;
import com.easyfinance.expenses.application.port.in.GetExpensePort;
import com.easyfinance.expenses.application.port.in.ListExpensesPort;
import com.easyfinance.expenses.application.port.in.UpdateExpensePort;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpensesControllerTest {

    private final CreateExpensePort createExpensePort = mock(CreateExpensePort.class);
    private final CreateInstallmentExpensePort createInstallmentExpensePort = mock(CreateInstallmentExpensePort.class);
    private final ListExpensesPort listExpensesPort = mock(ListExpensesPort.class);
    private final GetExpensePort getExpensePort = mock(GetExpensePort.class);
    private final UpdateExpensePort updateExpensePort = mock(UpdateExpensePort.class);
    private final CancelExpensePort cancelExpensePort = mock(CancelExpensePort.class);
    private final DuplicateExpensePort duplicateExpensePort = mock(DuplicateExpensePort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExpensesController(createExpensePort, createInstallmentExpensePort, listExpensesPort, getExpensePort, updateExpensePort, cancelExpensePort, duplicateExpensePort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void expenseCrudEndpointsDelegate() throws Exception {
        when(createExpensePort.createExpense(any())).thenReturn(expense());
        when(listExpensesPort.listExpenses(any())).thenReturn(new PageResponse<>(List.of(expense()), 0, 20, 1, 1));
        when(getExpensePort.getExpense(1L, 5L)).thenReturn(expense());
        when(updateExpensePort.updateExpense(any())).thenReturn(expense());
        when(duplicateExpensePort.duplicateExpense(any())).thenReturn(expense());

        mockMvc.perform(post("/api/v1/accounts/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"paymentMethodId":3,"description":"Lunch","amount":12000,"expenseDate":"2026-05-09","paymentState":"PAID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseType").value("SIMPLE"));

        mockMvc.perform(get("/api/v1/accounts/1/expenses?from=2026-05-01&to=2026-05-31&paymentState=PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Lunch"));

        mockMvc.perform(get("/api/v1/accounts/1/expenses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/accounts/1/expenses/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"paymentMethodId":3,"description":"Dinner","amount":15000,"expenseDate":"2026-05-09","paymentState":"PAID"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/accounts/1/expenses/5/cancel"))
                .andExpect(status().isNoContent());
        verify(cancelExpensePort).cancelExpense(1L, 5L);

        mockMvc.perform(post("/api/v1/accounts/1/expenses/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expenseDate":"2026-06-15","amount":85000,"description":"Mercado junio","paymentState":"PAID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseType").value("SIMPLE"));
    }

    @Test
    void validatesCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"paymentMethodId":3,"description":"","amount":0,"expenseDate":"2026-05-09"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void validatesDuplicateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/expenses/5/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listReceivesTrimmedSearchParam() throws Exception {
        when(listExpensesPort.listExpenses(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/1/expenses?search= Mercado "))
                .andExpect(status().isOk());

        ArgumentCaptor<ListExpensesQuery> captor = ArgumentCaptor.forClass(ListExpensesQuery.class);
        verify(listExpensesPort).listExpenses(captor.capture());
        assertThat(captor.getValue().search()).isEqualTo("Mercado");
    }

    @Test
    void listIgnoresBlankSearchParam() throws Exception {
        when(listExpensesPort.listExpenses(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/1/expenses?search=   "))
                .andExpect(status().isOk());

        ArgumentCaptor<ListExpensesQuery> captor = ArgumentCaptor.forClass(ListExpensesQuery.class);
        verify(listExpensesPort).listExpenses(captor.capture());
        assertThat(captor.getValue().search()).isNull();
    }

    @Test
    void listReceivesExpenseTypeParam() throws Exception {
        when(listExpensesPort.listExpenses(any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/accounts/1/expenses?expenseType=INSTALLMENT"))
                .andExpect(status().isOk());

        ArgumentCaptor<ListExpensesQuery> captor = ArgumentCaptor.forClass(ListExpensesQuery.class);
        verify(listExpensesPort).listExpenses(captor.capture());
        assertThat(captor.getValue().expenseType()).isEqualTo(ExpenseType.INSTALLMENT);
    }

    @Test
    void installmentExpenseEndpointDelegates() throws Exception {
        when(createInstallmentExpensePort.createInstallmentExpense(any())).thenReturn(installmentExpense());

        mockMvc.perform(post("/api/v1/accounts/1/expenses/installments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"paymentMethodId":3,"description":"Laptop","totalAmount":1200000,"expenseDate":"2026-05-11","installmentCount":6,"installmentAmount":200000,"firstInstallmentDate":"2026-06-01","debtName":"Laptop debt","notes":"No payments"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseType").value("INSTALLMENT"))
                .andExpect(jsonPath("$.paymentState").value("PENDING"));
    }

    private static ExpenseResponse expense() {
        return new ExpenseResponse(
                5L,
                1L,
                2L,
                3L,
                10L,
                "Lunch",
                new BigDecimal("12000.00"),
                "COP",
                LocalDate.of(2026, 5, 9),
                "PAID",
                "ACTIVE",
                "SIMPLE",
                Instant.now(),
                Instant.now()
        );
    }

    private static ExpenseResponse installmentExpense() {
        return new ExpenseResponse(
                6L,
                1L,
                2L,
                3L,
                10L,
                "Laptop",
                new BigDecimal("1200000.00"),
                "COP",
                LocalDate.of(2026, 5, 11),
                "PENDING",
                "ACTIVE",
                "INSTALLMENT",
                Instant.now(),
                Instant.now()
        );
    }
}
