package com.matteopaciolla.prbe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuthClientProvisioner implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Value("${pr.auth.web-client-id}")
    private String webClientId;
    @Value("${pr.auth.web-redirect-uris}")
    private String webRedirectUris;
    @Value("${pr.auth.web-post-logout-redirect-uris}")
    private String webPostLogoutRedirectUris;
    @Value("${pr.auth.cli-client-id}")
    private String cliClientId;
    @Value("${pr.auth.cli-redirect-uri}")
    private String cliRedirectUri;
    @Value("${pr.auth.bot-client-id}")
    private String botClientId;
    @Value("${pr.auth.bot-client-secret:}")
    private String botClientSecret;
    @Value("${pr.auth.access-token-ttl:PT5M}")
    private String accessTokenTtl;
    @Value("${pr.auth.refresh-token-ttl:PT8H}")
    private String refreshTokenTtl;

    @Override
    public void run(ApplicationArguments args) {
        provisionClient(webClient());
        provisionClient(cliClient());
        if (botClientSecret == null || botClientSecret.isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                throw new IllegalStateException(
                        "PORTROYAL_BOT_CLIENT_SECRET must be configured unless the dev or test profile is active.");
            }
            removeBotClient();
        } else {
            provisionClient(botClient());
        }
    }

    private RegisteredClient webClient() {
        Set<String> redirects = parseUris(webRedirectUris);
        Set<String> postLogoutRedirects = parseUris(webPostLogoutRedirectUris);
        boolean allowLocalRedirects = environment.acceptsProfiles(Profiles.of("dev", "test"));
        redirects.forEach(uri -> validateRedirect(uri, false, allowLocalRedirects));
        postLogoutRedirects.forEach(uri -> validateRedirect(uri, false, allowLocalRedirects));
        return publicClient(webClientId, "Port Royal browser client", redirects, postLogoutRedirects);
    }

    private RegisteredClient cliClient() {
        validateRedirect(cliRedirectUri, true, true);
        return publicClient(cliClientId, "Port Royal CLI", Set.of(cliRedirectUri), Set.of());
    }

    private RegisteredClient publicClient(
            String clientId, String clientName, Set<String> redirects, Set<String> postLogoutRedirects) {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scopes(scopes -> scopes.addAll(Set.of(
                        OidcScopes.OPENID,
                        OidcScopes.PROFILE,
                        OidcScopes.EMAIL,
                        "offline_access",
                        "portroyal-api")))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(tokenSettings());
        redirects.forEach(builder::redirectUri);
        postLogoutRedirects.forEach(builder::postLogoutRedirectUri);
        return builder.build();
    }

    private RegisteredClient botClient() {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(botClientId)
                .clientName("Port Royal Telegram bot")
                .clientSecret(passwordEncoder.encode(botClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("portroyal-api")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.parse(accessTokenTtl))
                        .build())
                .build();
    }

    private TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.parse(accessTokenTtl))
                .refreshTokenTimeToLive(Duration.parse(refreshTokenTtl))
                .reuseRefreshTokens(false)
                .build();
    }

    private void provisionClient(RegisteredClient configuredClient) {
        RegisteredClient existing = registeredClientRepository.findByClientId(configuredClient.getClientId());
        if (existing == null) {
            registeredClientRepository.save(configuredClient);
            return;
        }

        String secret = configuredClient.getClientSecret();
        if (secret != null && existing.getClientSecret() != null
                && passwordEncoder.matches(botClientSecret, existing.getClientSecret())) {
            secret = existing.getClientSecret();
        }
        String persistedSecret = secret;
        jdbcTemplate.update("""
                        UPDATE oauth2_registered_client
                        SET client_secret = ?, client_name = ?, client_authentication_methods = ?,
                            authorization_grant_types = ?, redirect_uris = ?, post_logout_redirect_uris = ?,
                            scopes = ?, client_settings = ?, token_settings = ?
                        WHERE id = ?
                        """,
                persistedSecret,
                configuredClient.getClientName(),
                commaSeparated(configuredClient.getClientAuthenticationMethods().stream()
                        .map(method -> method.getValue()).collect(Collectors.toSet())),
                commaSeparated(configuredClient.getAuthorizationGrantTypes().stream()
                        .map(grant -> grant.getValue()).collect(Collectors.toSet())),
                commaSeparated(configuredClient.getRedirectUris()),
                commaSeparated(configuredClient.getPostLogoutRedirectUris()),
                commaSeparated(configuredClient.getScopes()),
                writeJson(configuredClient.getClientSettings().getSettings()),
                writeJson(configuredClient.getTokenSettings().getSettings()),
                existing.getId());
    }

    private void removeBotClient() {
        jdbcTemplate.update("""
                DELETE FROM oauth2_authorization_consent
                WHERE registered_client_id IN (
                    SELECT id FROM oauth2_registered_client WHERE client_id = ?
                )
                """, botClientId);
        jdbcTemplate.update("""
                DELETE FROM oauth2_authorization
                WHERE registered_client_id IN (
                    SELECT id FROM oauth2_registered_client WHERE client_id = ?
                )
                """, botClientId);
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", botClientId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize OAuth client settings.", exception);
        }
    }

    private static Set<String> parseUris(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("OAuth client redirect URI configuration must not be empty.");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String commaSeparated(Set<String> values) {
        return String.join(",", values);
    }

    private static void validateRedirect(String value, boolean loopbackOnly, boolean allowLoopbackHttp) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalStateException("OAuth redirect URI must have a host and must not include userinfo or fragment.");
            }
            boolean loopback = host.equalsIgnoreCase("localhost")
                    || host.equals("127.0.0.1")
                    || host.equals("::1");
            if (loopbackOnly && !loopback) {
                throw new IllegalStateException("The CLI OAuth redirect URI must use a loopback host.");
            }
            if (!"https".equalsIgnoreCase(scheme)
                    && !(allowLoopbackHttp && loopback && "http".equalsIgnoreCase(scheme))) {
                throw new IllegalStateException("OAuth redirect URIs must use HTTPS except for loopback development callbacks.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid OAuth redirect URI configuration.", exception);
        }
    }
}
