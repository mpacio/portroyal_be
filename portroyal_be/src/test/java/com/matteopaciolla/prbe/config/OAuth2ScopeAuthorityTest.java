package com.matteopaciolla.prbe.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2ScopeAuthorityTest {

    private final OAuth2SecurityConfiguration configuration = new OAuth2SecurityConfiguration();

    @Test
    void humanRolesRequireGameScope() {
        var converter = configuration.jwtAuthenticationConverter();
        Jwt profileOnlyToken = token(List.of("ROLE_USER"), "openid profile");
        Jwt gameToken = token(List.of("ROLE_USER"), "openid profile game:play");

        assertThat(authorities(converter.convert(profileOnlyToken).getAuthorities()))
                .doesNotContain("ROLE_USER");
        assertThat(authorities(converter.convert(gameToken).getAuthorities()))
                .contains("ROLE_USER");
    }

    @Test
    void botRoleRequiresOnboardingScope() {
        var converter = configuration.jwtAuthenticationConverter();
        Jwt delegationOnly = token(List.of("ROLE_BOT_CLIENT"), "bot:delegate");
        Jwt onboarding = token(List.of("ROLE_BOT_CLIENT"), "bot:onboard");

        assertThat(authorities(converter.convert(delegationOnly).getAuthorities()))
                .doesNotContain("ROLE_BOT_CLIENT");
        assertThat(authorities(converter.convert(onboarding).getAuthorities()))
                .contains("ROLE_BOT_CLIENT");
    }

    private static Jwt token(List<String> roles, String scope) {
        return Jwt.withTokenValue("signed-token")
                .header("alg", "RS256")
                .subject("42")
                .claim("roles", roles)
                .claim("scope", scope)
                .build();
    }

    private static List<String> authorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
