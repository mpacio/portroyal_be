package com.matteopaciolla.prbe.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptRateLimiterTest {

    private LoginAttemptRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:login-rate-limit-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE auth_login_attempts (
                    attempt_key char(64) PRIMARY KEY,
                    attempt_count integer NOT NULL,
                    window_started_at timestamp with time zone NOT NULL,
                    blocked_until timestamp with time zone
                )
                """);
        rateLimiter = new LoginAttemptRateLimiter(jdbcTemplate);
    }

    @Test
    void blocksRepeatedBadPasswordsForTheAccountAndClearsTheAccountBucketAfterSuccess() {
        assertThat(rateLimiter.isAllowed("192.0.2.10", "alice42")).isTrue();

        for (int attempt = 0; attempt < 5; attempt++) {
            rateLimiter.recordFailure("192.0.2.10", "alice42");
        }

        assertThat(rateLimiter.isAllowed("192.0.2.10", "alice42")).isFalse();
        assertThat(rateLimiter.isAllowed("192.0.2.10", "bob42")).isTrue();

        rateLimiter.recordSuccess("alice42");

        assertThat(rateLimiter.isAllowed("192.0.2.10", "alice42")).isTrue();
    }
}
