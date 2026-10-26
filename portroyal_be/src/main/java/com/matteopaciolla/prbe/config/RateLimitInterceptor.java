package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RateLimitRule rule = getRule(request.getMethod(), request.getRequestURI());
        if (rule == null) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return true;
        }
        rateLimitService.enforce(
                rule.operation(),
                "account:" + authentication.getName(),
                rule.limit(),
                rule.window());
        return true;
    }

    private static RateLimitRule getRule(String method, String path) {
        if ("POST".equals(method) && "/api/v1/match/host".equals(path)) {
            return new RateLimitRule("match-create", 10, Duration.ofMinutes(1));
        }
        if ("POST".equals(method) && "/api/v1/match/join".equals(path)) {
            return new RateLimitRule("match-join", 10, Duration.ofMinutes(1));
        }
        if (isResourceRead(method, path)) {
            return new RateLimitRule("resource-read", 60, Duration.ofMinutes(1));
        }
        return null;
    }

    private static boolean isResourceRead(String method, String path) {
        if (!"GET".equals(method)) {
            return false;
        }
        return path.equals("/api/v1/match/retrieve-all")
                || path.equals("/api/v1/match/retrieve")
                || path.equals("/api/v1/match/status")
                || path.equals("/api/v1/game/move")
                || path.equals("/api/v1/game/moves")
                || path.equals("/api/v1/sentinel/long-polling/subscribe")
                || path.equals("/api/v1/sentinel/sse/subscribe");
    }

    private record RateLimitRule(String operation, int limit, Duration window) {
    }
}
