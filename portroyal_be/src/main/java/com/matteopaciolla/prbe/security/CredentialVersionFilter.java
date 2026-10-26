package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class CredentialVersionFilter extends OncePerRequestFilter {

    private static final Set<String> SESSION_AUTHENTICATION_PATHS =
            Set.of("/oauth2/authorize", "/oauth2/consent");

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SESSION_AUTHENTICATION_PATHS.contains(request.getRequestURI())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof UsernamePasswordAuthenticationToken
                    && authentication.isAuthenticated()
                    && !hasCurrentCredentials(authentication)) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasCurrentCredentials(Authentication authentication) {
        if (!(authentication.getDetails() instanceof CredentialVersion credentialVersion)) {
            return false;
        }
        return userRepository.findById(credentialVersion.userId())
                .filter(UserEntity::isEnabled)
                .filter(UserEntity::isEmailConfirmed)
                .filter(user -> user.getCredentialVersion() == credentialVersion.version())
                .isPresent();
    }
}
