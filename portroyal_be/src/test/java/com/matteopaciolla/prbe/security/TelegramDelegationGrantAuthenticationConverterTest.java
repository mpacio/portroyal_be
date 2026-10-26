package com.matteopaciolla.prbe.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramDelegationGrantAuthenticationConverterTest {

    private final TelegramDelegationGrantAuthenticationConverter converter =
            new TelegramDelegationGrantAuthenticationConverter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsTelegramIdOnlyFromTheDelegationGrantParameter() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "telegram-bot", null, List.of(new SimpleGrantedAuthority("ROLE_BOT_CLIENT"))));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setParameter("grant_type", TelegramDelegationGrantAuthenticationToken.GRANT_TYPE.getValue());
        request.setParameter("tgId", "123456789");

        var authentication = converter.convert(request);

        assertThat(authentication).isInstanceOf(TelegramDelegationGrantAuthenticationToken.class);
        assertThat(((TelegramDelegationGrantAuthenticationToken) authentication).getTelegramId())
                .isEqualTo("123456789");
    }

    @Test
    void rejectsTelegramIdOnOtherTokenGrantsAndInHeaders() {
        MockHttpServletRequest wrongGrant = new MockHttpServletRequest("POST", "/oauth2/token");
        wrongGrant.setParameter("grant_type", "client_credentials");
        wrongGrant.setParameter("tgId", "123456789");
        assertThatThrownBy(() -> converter.convert(wrongGrant))
                .isInstanceOf(OAuth2AuthenticationException.class);

        MockHttpServletRequest headerGrant = new MockHttpServletRequest("POST", "/oauth2/token");
        headerGrant.setParameter("grant_type", TelegramDelegationGrantAuthenticationToken.GRANT_TYPE.getValue());
        headerGrant.addHeader("tgId", "123456789");
        assertThatThrownBy(() -> converter.convert(headerGrant))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
