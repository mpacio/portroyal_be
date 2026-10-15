package com.matteopaciolla.prbe.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public final class PublicRefreshClientAuthenticationConverter implements AuthenticationConverter {

    static final String MARKER = "portroyal_public_refresh_client";
    static final String REVOCATION_MARKER = "portroyal_public_refresh_revocation_client";

    @Override
    public Authentication convert(HttpServletRequest request) {
        boolean refreshGrant = "refresh_token".equals(request.getParameter("grant_type"));
        boolean refreshRevocation = "refresh_token".equals(request.getParameter("token_type_hint"))
                && request.getParameter("token") != null;
        if (!refreshGrant && !refreshRevocation) {
            return null;
        }
        String clientId = request.getParameter("client_id");
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        String marker = refreshGrant ? MARKER : REVOCATION_MARKER;
        return new OAuth2ClientAuthenticationToken(
                clientId,
                org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE,
                null,
                Map.of(marker, Boolean.TRUE));
    }
}
