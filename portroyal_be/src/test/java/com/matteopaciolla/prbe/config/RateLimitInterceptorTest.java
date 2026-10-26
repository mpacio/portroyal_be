package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.service.RateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitInterceptorTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimitService);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchCreationUsesTheAuthenticatedAccountScope() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("42", null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/match/host");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verify(rateLimitService).enforce(
                "match-create", "account:42", 10, java.time.Duration.ofMinutes(1));
    }

    @Test
    void nonResourceReadDoesNotConsumeAccountReadQuota() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("42", null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/me");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }
}
