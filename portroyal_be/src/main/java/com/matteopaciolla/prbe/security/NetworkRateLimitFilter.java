package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import com.matteopaciolla.prbe.exceptions.common.RateLimitExceededException;
import com.matteopaciolla.prbe.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
public class NetworkRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        List<RateLimitRule> rules = getRules(request);
        try {
            for (RateLimitRule rule : rules) {
                rateLimitService.enforce(rule.operation(), rule.subject(), rule.limit(), rule.window());
            }
        } catch (RateLimitExceededException exception) {
            writeRateLimitResponse(response, exception.getRetryAfterSeconds());
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return getRules(request).isEmpty();
    }

    private List<RateLimitRule> getRules(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String remoteAddress = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        List<RateLimitRule> rules = new ArrayList<>();

        if ("POST".equals(method)
                && ("/api/v1/public/register".equals(path)
                || "/api/v1/user/register/telegram".equals(path))) {
            rules.add(new RateLimitRule("registration", remoteAddress, 5, Duration.ofHours(1)));
        }
        if ("POST".equals(method) && "/oauth2/token".equals(path)) {
            rules.add(new RateLimitRule("token-issuance", remoteAddress, 10, Duration.ofMinutes(1)));
            if ("refresh_token".equals(request.getParameter("grant_type"))) {
                String clientId = getClientId(request);
                rules.add(new RateLimitRule(
                        "token-refresh", clientId + "|" + remoteAddress, 10, Duration.ofMinutes(1)));
            }
        }
        if (("GET".equals(method) || "POST".equals(method)) && "/oauth2/authorize".equals(path)) {
            rules.add(new RateLimitRule("authorization", remoteAddress, 10, Duration.ofMinutes(1)));
        }
        if ("POST".equals(method) && "/perform-login".equals(path)) {
            rules.add(new RateLimitRule("login", remoteAddress, 10, Duration.ofMinutes(1)));
        }
        return rules;
    }

    private static String getClientId(HttpServletRequest request) {
        String clientId = request.getParameter("client_id");
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic ")) {
            try {
                String credentials = new String(
                        Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
                int separator = credentials.indexOf(':');
                if (separator > 0) {
                    return credentials.substring(0, separator);
                }
            } catch (IllegalArgumentException ignored) {
                return "invalid-client";
            }
        }
        return "unknown-client";
    }

    private record RateLimitRule(String operation, String subject, int limit, Duration window) {
    }

    private void writeRateLimitResponse(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Integer.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "Request rate limit exceeded"));
    }
}
