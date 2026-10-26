package com.matteopaciolla.prbe.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigCorsTest {

    @Test
    void corsAllowsOnlyConfiguredOriginsAndRequiredHeadersWithoutCredentials() {
        CorsConfigurationSource source =
                new WebSecurityConfig().corsConfigurationSource("https://client.example.com, https://mobile.example.com");

        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/match/status"));

        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://client.example.com", "https://mobile.example.com");
        assertThat(configuration.getAllowedHeaders())
                .containsExactly("Authorization", "Content-Type", "Accept", "tgId");
        assertThat(configuration.getExposedHeaders()).containsExactly("X-Request-ID", "Retry-After");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void corsDeniesCrossOriginRequestsWhenNoOriginsAreConfigured() {
        CorsConfigurationSource source = new WebSecurityConfig().corsConfigurationSource("");

        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/match/status"));

        assertThat(configuration.checkOrigin("https://unconfigured.example.com")).isNull();
    }
}
