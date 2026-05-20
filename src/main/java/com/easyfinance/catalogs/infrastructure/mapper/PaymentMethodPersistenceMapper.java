package com.easyfinance.catalogs.infrastructure.mapper;

import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CatalogStatusJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodTypeJpa;

public class PaymentMethodPersistenceMapper {

    public PaymentMethod toDomain(PaymentMethodJpaEntity entity) {
        return PaymentMethod.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getName(),
                entity.getDescription(),
                PaymentMethodType.valueOf(entity.getType().name()),
                CatalogStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentMethodJpaEntity toEntity(PaymentMethod paymentMethod) {
        PaymentMethodJpaEntity entity = new PaymentMethodJpaEntity();
        copyToEntity(paymentMethod, entity);
        return entity;
    }

    public void copyToEntity(PaymentMethod paymentMethod, PaymentMethodJpaEntity entity) {
        entity.setId(paymentMethod.id());
        entity.setAccountId(paymentMethod.accountId());
        entity.setName(paymentMethod.name());
        entity.setNormalizedName(paymentMethod.normalizedName());
        entity.setDescription(paymentMethod.description());
        entity.setType(PaymentMethodTypeJpa.valueOf(paymentMethod.type().name()));
        entity.setStatus(CatalogStatusJpa.valueOf(paymentMethod.status().name()));
    }
}
