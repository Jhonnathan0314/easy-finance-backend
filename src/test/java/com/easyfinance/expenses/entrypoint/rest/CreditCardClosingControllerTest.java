package com.easyfinance.expenses.entrypoint.rest;

import com.easyfinance.expenses.application.port.in.ConfirmCreditCardClosingPort;
import com.easyfinance.expenses.application.port.in.PreviewCreditCardClosingPort;
import com.easyfinance.expenses.application.response.CreditCardClosingCategoryItem;
import com.easyfinance.expenses.application.response.CreditCardClosingPreviewResponse;
import com.easyfinance.expenses.application.response.CreditCardClosingResultResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditCardClosingControllerTest {

    private final PreviewCreditCardClosingPort previewCreditCardClosingPort = mock(PreviewCreditCardClosingPort.class);
    private final ConfirmCreditCardClosingPort confirmCreditCardClosingPort = mock(ConfirmCreditCardClosingPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CreditCardClosingController(previewCreditCardClosingPort, confirmCreditCardClosingPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void previewDelegatesToPortAndReturnsSummary() throws Exception {
        when(previewCreditCardClosingPort.previewClosing(any())).thenReturn(new CreditCardClosingPreviewResponse(
                3L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("35000"),
                2,
                List.of(new CreditCardClosingCategoryItem(2L, "Groceries", new BigDecimal("35000"), 2)),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/accounts/1/expenses/credit-card-closing/preview")
                        .param("paymentMethodId", "3")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(35000))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.byCategory[0].categoryName").value("Groceries"));
    }

    @Test
    void confirmDelegatesToPortAndReturnsResult() throws Exception {
        when(confirmCreditCardClosingPort.confirmClosing(any())).thenReturn(new CreditCardClosingResultResponse(
                3L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("35000"),
                2
        ));

        mockMvc.perform(post("/api/v1/accounts/1/expenses/credit-card-closing/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethodId":3,"from":"2026-05-01","to":"2026-05-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(35000));
    }
}
