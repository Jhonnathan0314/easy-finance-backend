package com.easyfinance.identity.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class IdentitySchemaIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void flywayCreatesGlobalRoles() {
        Integer roles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM global_roles WHERE name IN ('USER', 'SUPER_ADMIN')",
                Integer.class
        );

        assertThat(roles).isEqualTo(2);
    }

    @Test
    void usersEmailIsUnique() {
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?)",
                "unique@example.com",
                "$2a$hash",
                "Unique User",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?)",
                "unique@example.com",
                "$2a$hash",
                "Other User",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void participantsUserIdIsUnique() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "participant@example.com",
                "$2a$hash",
                "Participant User",
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?)",
                userId,
                "Participant User",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?)",
                userId,
                "Duplicate Participant",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }
}
