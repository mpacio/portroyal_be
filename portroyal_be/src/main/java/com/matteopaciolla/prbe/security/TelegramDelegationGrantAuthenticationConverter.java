package com.matteopaciolla.prbe.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationConverter;

public final class TelegramDelegationGrantAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        String telegramId = request.getParameter("tgId");
        if (request.getHeader("tgId") != null
                || (telegramId != null
                && !TelegramDelegationGrantAuthenticationToken.GRANT_TYPE.getValue().equals(grantType))) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
        if (!TelegramDelegationGrantAuthenticationToken.GRANT_TYPE.getValue().equals(grantType)) {
            return null;
        }
        return new TelegramDelegationGrantAuthenticationToken(
                SecurityContextHolder.getContext().getAuthentication(),
                telegramId);
    }
}
