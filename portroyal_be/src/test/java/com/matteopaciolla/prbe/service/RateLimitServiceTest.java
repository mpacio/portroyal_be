package com.matteopaciolla.prbe.service;

import com.matteopaciolla.prbe.exceptions.common.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void exceedingLimitIncludesTheRetryWindow() {
        when(jdbcTemplate.queryForObject(
                anyString(), anyRowMapper(), any(Object[].class)))
                .thenReturn(new RateLimitService.BucketUsage(11, 23));
        RateLimitService service = new RateLimitService(jdbcTemplate, new SimpleMeterRegistry());

        assertThatThrownBy(() -> service.enforce("token-issuance", "198.51.100.2", 10, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting(exception -> ((RateLimitExceededException) exception).getRetryAfterSeconds())
                .isEqualTo(23);
    }

    @Test
    void bucketScopesAreHashedBeforePersistence() {
        when(jdbcTemplate.queryForObject(
                anyString(), anyRowMapper(), any(Object[].class)))
                .thenReturn(new RateLimitService.BucketUsage(1, 60));
        RateLimitService service = new RateLimitService(jdbcTemplate, new SimpleMeterRegistry());
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);

        service.enforceEmailSend("Alice@example.com", "198.51.100.2");

        verify(jdbcTemplate).queryForObject(anyString(), anyRowMapper(), arguments.capture());
        assertThat(arguments.getValue())
                .noneMatch(value -> "Alice@example.com|198.51.100.2".equals(value));
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<RateLimitService.BucketUsage> anyRowMapper() {
        return any(RowMapper.class);
    }
}
