package com.easyfinance.accounts.infrastructure.persistence;

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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class AccountsSchemaIT {

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
    void accountParticipantIsUniquePerAccount() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "account-member@example.com",
                "$2a$hash",
                "Account Member",
                "ACTIVE"
        );
        Long participantId = jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Account Member",
                "ACTIVE"
        );
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Home",
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_ADMIN",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_MEMBER",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void accountRoleConstraintRejectsInvalidValue() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "invalid-role@example.com",
                "$2a$hash",
                "Invalid Role",
                "ACTIVE"
        );
        Long participantId = jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Invalid Role",
                "ACTIVE"
        );
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Business",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "OWNER",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void accountCannotBePhysicallyDeletedWhileItHasMemberships() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "restrict-delete@example.com",
                "$2a$hash",
                "Restrict Delete",
                "ACTIVE"
        );
        Long participantId = jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Restrict Delete",
                "ACTIVE"
        );
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Protected",
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_ADMIN",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM accounts WHERE id = ?", accountId))
                .isInstanceOf(DataAccessException.class);
    }
}
