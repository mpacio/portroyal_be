package com.matteopaciolla.prbe.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class LoginAttemptRateLimiter implements InitializingBean {

    private static final AtomicLong NEXT_CLEANUP_AT = new AtomicLong();
    private final JdbcTemplate jdbcTemplate;

    @Value("${pr.auth.login-rate-limit.window:PT15M}")
    private Duration window = Duration.ofMinutes(15);
    @Value("${pr.auth.login-rate-limit.account-attempts:5}")
    private int accountAttemptLimit = 5;
    @Value("${pr.auth.login-rate-limit.address-attempts:100}")
    private int addressAttemptLimit = 100;

    @Override
    public void afterPropertiesSet() {
        if (window == null || window.isZero() || window.isNegative()
                || accountAttemptLimit < 1 || addressAttemptLimit < 1) {
            throw new IllegalStateException("Login rate-limit window and thresholds must all be positive.");
        }
    }

    public boolean isAllowed(String remoteAddress, String username) {
        return isAllowed(key("address", remoteAddress))
                && isAllowed(key("account", username));
    }

    public void recordFailure(String remoteAddress, String username) {
        Instant now = Instant.now();
        cleanupExpiredAttempts(now);
        recordFailure(key("address", remoteAddress), addressAttemptLimit, now);
        recordFailure(key("account", username), accountAttemptLimit, now);
    }

    public void recordSuccess(String username) {
        jdbcTemplate.update("DELETE FROM auth_login_attempts WHERE attempt_key = ?",
                key("account", username));
    }

    private boolean isAllowed(String attemptKey) {
        return jdbcTemplate.query("""
                        SELECT attempt_count, window_started_at, blocked_until
                        FROM auth_login_attempts
                        WHERE attempt_key = ?
                        """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return true;
                    }
                    Instant startedAt = resultSet.getTimestamp("window_started_at").toInstant();
                    if (startedAt.plus(window).isBefore(Instant.now())) {
                        return true;
                    }
                    Timestamp blockedUntil = resultSet.getTimestamp("blocked_until");
                    return blockedUntil == null || !Instant.now().isBefore(blockedUntil.toInstant());
                },
                attemptKey);
    }

    private void recordFailure(String attemptKey, int limit, Instant now) {
        Instant windowStartCutoff = now.minus(window);
        Instant blockedUntil = now.plus(window);
        int updated = jdbcTemplate.update("""
                        UPDATE auth_login_attempts
                        SET attempt_count = CASE
                                WHEN window_started_at < ? THEN 1
                                ELSE attempt_count + 1
                            END,
                            blocked_until = CASE
                                WHEN window_started_at < ? THEN NULL
                                WHEN attempt_count + 1 >= ? THEN ?
                                ELSE blocked_until
                            END,
                            window_started_at = CASE
                                WHEN window_started_at < ? THEN ?
                                ELSE window_started_at
                            END
                        WHERE attempt_key = ?
                        """,
                Timestamp.from(windowStartCutoff),
                Timestamp.from(windowStartCutoff),
                limit,
                Timestamp.from(blockedUntil),
                Timestamp.from(windowStartCutoff),
                Timestamp.from(now),
                attemptKey);
        if (updated != 0) {
            return;
        }

        try {
            jdbcTemplate.update("""
                            INSERT INTO auth_login_attempts
                                (attempt_key, attempt_count, window_started_at, blocked_until)
                            VALUES (?, 1, ?, NULL)
                            """,
                    attemptKey, Timestamp.from(now));
        } catch (DuplicateKeyException concurrentInsert) {
            recordFailure(attemptKey, limit, now);
        }
    }

    private void cleanupExpiredAttempts(Instant now) {
        long nextCleanupAt = NEXT_CLEANUP_AT.get();
        if (now.toEpochMilli() < nextCleanupAt
                || !NEXT_CLEANUP_AT.compareAndSet(nextCleanupAt, now.plus(Duration.ofMinutes(10)).toEpochMilli())) {
            return;
        }
        jdbcTemplate.update("DELETE FROM auth_login_attempts WHERE window_started_at < ?",
                Timestamp.from(now.minus(window.multipliedBy(2))));
    }

    private static String key(String type, String value) {
        String normalized = value == null || value.isBlank()
                ? "unknown"
                : value.trim().toLowerCase(Locale.ROOT);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((type + ":" + normalized).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
