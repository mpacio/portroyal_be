package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.exceptions.common.RateLimitExceededException;
import com.matteopaciolla.prbe.util.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String ACQUIRE_BUCKET_SQL = """
            WITH current_window AS (
                SELECT to_timestamp(floor(extract(epoch FROM clock_timestamp()) / ?) * ?) AS window_started_at
            )
            INSERT INTO api_rate_limit_buckets (
                bucket_key, operation, scope_hash, window_started_at, request_count, expires_at, updated_at)
            SELECT ?, ?, ?, current_window.window_started_at, 1,
                   current_window.window_started_at + (? * INTERVAL '1 second'), clock_timestamp()
            FROM current_window
            ON CONFLICT (bucket_key) DO UPDATE
            SET request_count = CASE
                    WHEN api_rate_limit_buckets.window_started_at = EXCLUDED.window_started_at
                    THEN api_rate_limit_buckets.request_count + 1
                    ELSE 1
                END,
                window_started_at = EXCLUDED.window_started_at,
                expires_at = EXCLUDED.expires_at,
                updated_at = clock_timestamp()
            RETURNING api_rate_limit_buckets.request_count,
                GREATEST(1, CEIL(EXTRACT(EPOCH FROM (
                    api_rate_limit_buckets.window_started_at + (? * INTERVAL '1 second') - clock_timestamp()
                ))))::integer AS retry_after_seconds
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public void enforce(String operation, String scope, int limit, Duration window) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(window);
        if (limit <= 0 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit and window must be positive");
        }

        long windowSeconds = window.toSeconds();
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("Rate limit window must be at least one second");
        }

        String scopeHash = PasswordResetToken.hash(scope);
        String bucketKey = PasswordResetToken.hash(operation + ":" + scopeHash);
        BucketUsage usage = jdbcTemplate.queryForObject(
                ACQUIRE_BUCKET_SQL,
                (resultSet, rowNumber) -> new BucketUsage(
                        resultSet.getInt("request_count"),
                        resultSet.getInt("retry_after_seconds")),
                windowSeconds,
                windowSeconds,
                bucketKey,
                operation,
                scopeHash,
                windowSeconds,
                windowSeconds);
        if (usage == null) {
            throw new IllegalStateException("Rate limit store did not return the updated bucket");
        }
        if (usage.requestCount() > limit) {
            meterRegistry.counter("portroyal_rate_limit_exceeded", "operation", operation).increment();
            throw new RateLimitExceededException(usage.retryAfterSeconds());
        }
    }

    public void enforceEmailSend(String email, String remoteAddress) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("An email address is required for an email-send rate limit");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String sourceAddress = remoteAddress == null ? "unknown" : remoteAddress;
        enforce("email-send", normalizedEmail + "|" + sourceAddress, 3, Duration.ofHours(1));
    }

    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredBuckets() {
        jdbcTemplate.update(
                "DELETE FROM api_rate_limit_buckets WHERE expires_at < clock_timestamp() - INTERVAL '1 day'");
    }

    record BucketUsage(int requestCount, int retryAfterSeconds) {
    }
}
