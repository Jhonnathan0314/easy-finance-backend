package com.easyfinance.shared.infrastructure.audit;

import com.easyfinance.shared.application.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware(CurrentUserProvider currentUserProvider) {
        return () -> currentUserProvider.currentUserId().or(Optional::empty);
    }
}
