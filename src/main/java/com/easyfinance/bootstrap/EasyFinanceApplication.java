package com.easyfinance.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.easyfinance")
@EntityScan(basePackages = "com.easyfinance")
@EnableJpaRepositories(basePackages = "com.easyfinance")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class EasyFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyFinanceApplication.class, args);
    }
}
