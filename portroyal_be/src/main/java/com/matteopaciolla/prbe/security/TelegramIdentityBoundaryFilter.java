package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public final class TelegramIdentityBoundaryFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean hasHeader = request.getHeader("tgId") != null;
        boolean hasParameter = request.getParameter("tgId") != null;
        boolean acceptedHeader = hasHeader && !hasParameter && isAuthorizedAccountFlow(request);
        if ((hasHeader || hasParameter) && !acceptedHeader) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(),
                    new ErrorResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "Telegram identity is accepted only for BOT-authorized account flows."));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isAuthorizedAccountFlow(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.equals("/api/v1/user/register/telegram")
                || path.equals("/api/v1/user/unify")
                || path.equals("/api/v1/user/new-tg-unify-email-otp");
    }

}
