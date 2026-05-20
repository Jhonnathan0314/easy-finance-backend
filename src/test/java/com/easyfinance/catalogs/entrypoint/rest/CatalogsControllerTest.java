package com.easyfinance.catalogs.entrypoint.rest;

import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.DeactivateCategoryPort;
import com.easyfinance.catalogs.application.port.in.DeactivatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.port.in.GetPaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.ListCategoriesPort;
import com.easyfinance.catalogs.application.port.in.ListPaymentMethodsPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.application.port.in.UpdateCategoryPort;
import com.easyfinance.catalogs.application.port.in.UpdatePaymentMethodPort;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogsControllerTest {

    private final CreateCategoryPort createCategoryPort = mock(CreateCategoryPort.class);
    private final ListCategoriesPort listCategoriesPort = mock(ListCategoriesPort.class);
    private final GetCategoryPort getCategoryPort = mock(GetCategoryPort.class);
    private final UpdateCategoryPort updateCategoryPort = mock(UpdateCategoryPort.class);
    private final DeactivateCategoryPort deactivateCategoryPort = mock(DeactivateCategoryPort.class);
    private final CreatePaymentMethodPort createPaymentMethodPort = mock(CreatePaymentMethodPort.class);
    private final ListPaymentMethodsPort listPaymentMethodsPort = mock(ListPaymentMethodsPort.class);
    private final GetPaymentMethodPort getPaymentMethodPort = mock(GetPaymentMethodPort.class);
    private final UpdatePaymentMethodPort updatePaymentMethodPort = mock(UpdatePaymentMethodPort.class);
    private final DeactivatePaymentMethodPort deactivatePaymentMethodPort = mock(DeactivatePaymentMethodPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CategoriesController(createCategoryPort, listCategoriesPort, getCategoryPort, updateCategoryPort, deactivateCategoryPort),
                        new PaymentMethodsController(createPaymentMethodPort, listPaymentMethodsPort, getPaymentMethodPort, updatePaymentMethodPort, deactivatePaymentMethodPort)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void categoryCrudEndpointsDelegate() throws Exception {
        when(createCategoryPort.createCategory(any())).thenReturn(category());
        when(listCategoriesPort.listCategories(any())).thenReturn(new PageResponse<>(List.of(category()), 0, 20, 1, 1));
        when(getCategoryPort.getCategory(1L, 2L)).thenReturn(category());
        when(updateCategoryPort.updateCategory(any())).thenReturn(category());

        mockMvc.perform(post("/api/v1/accounts/1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"Food","description":"Meals","type":"EXPENSE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        mockMvc.perform(get("/api/v1/accounts/1/categories?type=EXPENSE&sort=name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Food"));

        mockMvc.perform(get("/api/v1/accounts/1/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food"));

        mockMvc.perform(put("/api/v1/accounts/1/categories/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"Food","description":"Meals"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/accounts/1/categories/2"))
                .andExpect(status().isNoContent());
        verify(deactivateCategoryPort).deactivateCategory(1L, 2L);
    }

    @Test
    void paymentMethodCrudEndpointsDelegate() throws Exception {
        when(createPaymentMethodPort.createPaymentMethod(any())).thenReturn(paymentMethod());
        when(listPaymentMethodsPort.listPaymentMethods(any())).thenReturn(new PageResponse<>(List.of(paymentMethod()), 0, 20, 1, 1));
        when(getPaymentMethodPort.getPaymentMethod(1L, 2L)).thenReturn(paymentMethod());
        when(updatePaymentMethodPort.updatePaymentMethod(any())).thenReturn(paymentMethod());

        mockMvc.perform(post("/api/v1/accounts/1/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"Cash","description":"Wallet","type":"CASH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CASH"));

        mockMvc.perform(get("/api/v1/accounts/1/payment-methods?type=CASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cash"));

        mockMvc.perform(get("/api/v1/accounts/1/payment-methods/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cash"));

        mockMvc.perform(put("/api/v1/accounts/1/payment-methods/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"Cash","description":"Wallet"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/accounts/1/payment-methods/2"))
                .andExpect(status().isNoContent());
        verify(deactivatePaymentMethodPort).deactivatePaymentMethod(1L, 2L);
    }

    @Test
    void validatesRequests() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"","type":"EXPENSE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void categoriesListReceivesSearchQueryParam() throws Exception {
        when(listCategoriesPort.listCategories(any())).thenReturn(new PageResponse<>(List.of(category()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts/1/categories")
                        .param("search", " Mercado ")
                        .param("type", "EXPENSE")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());

        var captor = forClass(ListCategoriesQuery.class);
        verify(listCategoriesPort).listCategories(captor.capture());
        assertThat(captor.getValue().search()).isEqualTo("Mercado");
    }

    @Test
    void paymentMethodsListReceivesSearchQueryParam() throws Exception {
        when(listPaymentMethodsPort.listPaymentMethods(any())).thenReturn(new PageResponse<>(List.of(paymentMethod()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts/1/payment-methods")
                        .param("search", " visa ")
                        .param("type", "CREDIT_CARD")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());

        var captor = forClass(ListPaymentMethodsQuery.class);
        verify(listPaymentMethodsPort).listPaymentMethods(captor.capture());
        assertThat(captor.getValue().search()).isEqualTo("visa");
    }

    @Test
    void blankSearchIsNormalizedToNull() throws Exception {
        when(listCategoriesPort.listCategories(any())).thenReturn(new PageResponse<>(List.of(category()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/accounts/1/categories")
                        .param("search", "   "))
                .andExpect(status().isOk());

        var captor = forClass(ListCategoriesQuery.class);
        verify(listCategoriesPort).listCategories(captor.capture());
        assertThat(captor.getValue().search()).isNull();
    }

    private static CategoryResponse category() {
        return new CategoryResponse(2L, 1L, "Food", "Meals", "EXPENSE", "ACTIVE", Instant.now(), Instant.now());
    }

    private static PaymentMethodResponse paymentMethod() {
        return new PaymentMethodResponse(2L, 1L, "Cash", "Wallet", "CASH", "ACTIVE", Instant.now(), Instant.now());
    }
}
