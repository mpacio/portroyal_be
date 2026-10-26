package com.matteopaciolla.prbe.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesHumanUsernameFromStableSubjectToken() {
        Jwt jwt = Jwt.withTokenValue("signed-token")
                .header("alg", "RS256")
                .subject("42")
                .claim("username", "alice")
                .claim("actor_client_id", "telegram-bot")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(AuthenticationUtils.getUsername()).isEqualTo("alice");
        assertThat(AuthenticationUtils.isBotActor()).isTrue();
        assertThat(AuthenticationUtils.isBotUser()).isFalse();
    }

    @Test
    void identifiesBotClientWithoutTreatingItAsAHuman() {
        Jwt jwt = Jwt.withTokenValue("signed-token")
                .header("alg", "RS256")
                .subject("telegram-bot")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_BOT_CLIENT"))));

        assertThat(AuthenticationUtils.isBotUser()).isTrue();
        assertThatThrownBy(AuthenticationUtils::getUsername)
                .isInstanceOf(IllegalStateException.class);
    }
}
