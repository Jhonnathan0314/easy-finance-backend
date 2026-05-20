package com.easyfinance.catalogs.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodTest {

    @Test
    void createsActivePaymentMethod() {
        PaymentMethod paymentMethod = PaymentMethod.create(1L, " Cash ", " Wallet ", PaymentMethodType.CASH);

        assertThat(paymentMethod.name()).isEqualTo("Cash");
        assertThat(paymentMethod.normalizedName()).isEqualTo("cash");
        assertThat(paymentMethod.description()).isEqualTo("Wallet");
        assertThat(paymentMethod.status()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> PaymentMethod.create(1L, " ", null, PaymentMethodType.CASH))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_NAME_REQUIRED"));
    }

    @Test
    void inactivePaymentMethodCannotBeUpdated() {
        PaymentMethod inactive = PaymentMethod.restore(1L, 1L, "Cash", null, PaymentMethodType.CASH, CatalogStatus.INACTIVE, null, null);

        assertThatThrownBy(() -> inactive.update("Cash 2", null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_INACTIVE"));
    }
}
