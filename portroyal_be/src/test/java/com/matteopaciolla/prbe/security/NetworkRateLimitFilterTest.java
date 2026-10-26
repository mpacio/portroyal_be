package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.service.RateLimitService;
import com.matteopaciolla.prbe.exceptions.common.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NetworkRateLimitFilterTest {

    @Test
    void registrationIsLimitedBySourceIp() throws Exception {
        RateLimitService rateLimitService = mock(RateLimitService.class);
        NetworkRateLimitFilter filter = new NetworkRateLimitFilter(rateLimitService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/register");
        request.setRemoteAddr("198.51.100.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> continued.set(true));

        verify(rateLimitService).enforce(
                "registration", "198.51.100.4", 5, Duration.ofHours(1));
        assertThat(continued).isTrue();
    }

    @Test
    void exceededIpLimitReturns429AndRetryAfterWithoutInvokingTheApplication() throws Exception {
        RateLimitService rateLimitService = mock(RateLimitService.class);
        doThrow(new RateLimitExceededException(30)).when(rateLimitService)
                .enforce("registration", "198.51.100.4", 5, Duration.ofHours(1));
        NetworkRateLimitFilter filter = new NetworkRateLimitFilter(rateLimitService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/register");
        request.setRemoteAddr("198.51.100.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> continued.set(true));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("Request rate limit exceeded");
        assertThat(continued).isFalse();
    }

    @Test
    void tokenRefreshUsesOAuthClientAndSourceIpScopeWithoutConsumingFormParameters() throws Exception {
        RateLimitService rateLimitService = mock(RateLimitService.class);
        NetworkRateLimitFilter filter = new NetworkRateLimitFilter(rateLimitService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setRemoteAddr("198.51.100.9");
        request.setContentType("application/x-www-form-urlencoded");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.addParameter("grant_type", "refresh_token");
        request.addParameter("client_id", "public-client");
        request.setContent("grant_type=refresh_token&client_id=public-client&refresh_token=secret"
                .getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(((HttpServletRequest) servletRequest).getParameter("grant_type"))
                    .isEqualTo("refresh_token");
        });

        verify(rateLimitService).enforce(
                "token-issuance", "198.51.100.9", 10, Duration.ofMinutes(1));
        verify(rateLimitService).enforce(
                "token-refresh", "public-client|198.51.100.9", 10, Duration.ofMinutes(1));
    }
}
