package com.matteopaciolla.prbe.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Collections;

public final class TelegramDelegationGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType GRANT_TYPE =
            new AuthorizationGrantType("urn:portroyal:grant-type:telegram-delegation");

    private final String telegramId;

    public TelegramDelegationGrantAuthenticationToken(Authentication clientPrincipal, String telegramId) {
        super(GRANT_TYPE, clientPrincipal, Collections.singletonMap("tgId", telegramId));
        this.telegramId = telegramId;
    }

    public String getTelegramId() {
        return telegramId;
    }
}
