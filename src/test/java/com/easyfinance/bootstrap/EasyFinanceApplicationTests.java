package com.easyfinance.bootstrap;

import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.easyfinance.shared.infrastructure.security.RestSecurityExceptionHandler;
import com.easyfinance.shared.infrastructure.security.JwtAuthenticationFilter;
import com.easyfinance.shared.infrastructure.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EasyFinanceApplicationTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestSecurityExceptionHandler restSecurityExceptionHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
        assertThat(jwtTokenService).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
        assertThat(springDataUserRepository).isNotNull();
        assertThat(objectMapper).isNotNull();
        assertThat(restSecurityExceptionHandler).isNotNull();
        assertThat(appliedFlywayMigrations()).isGreaterThanOrEqualTo(13);
        assertThat(tableExists("account_participants")).isTrue();
    }

    private Integer appliedFlywayMigrations() {
        return jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
    }

    private Boolean tableExists(String tableName) {
        return jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = ?)",
                Boolean.class,
                tableName
        );
    }
}
