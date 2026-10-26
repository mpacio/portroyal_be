package com.matteopaciolla.prbe.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthenticationUtils {

    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String username = jwtAuthentication.getToken().getClaimAsString("username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            throw new IllegalStateException("The authenticated token does not represent a human user");
        }
        return authentication.getName();
    }

    public static boolean isBotUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BOT_CLIENT"));
    }

    public static boolean isBotActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getClaimAsString("actor_client_id") != null;
        }
        return false;
    }
}
