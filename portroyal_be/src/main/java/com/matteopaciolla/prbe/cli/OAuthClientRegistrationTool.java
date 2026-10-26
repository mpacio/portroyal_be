package com.matteopaciolla.prbe.cli;

import com.matteopaciolla.prbe.security.TelegramDelegationGrantAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class OAuthClientRegistrationTool {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private OAuthClientRegistrationTool() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "Usage: OAuthClientRegistrationTool public <client-id> <exact-redirect-uri>... | bot | rotate-bot");
        }
        ClientStore store = createStore();
        switch (args[0]) {
            case "public" -> registerPublicClient(store.registeredClientRepository(), args);
            case "bot" -> registerBotClient(store.registeredClientRepository(), System.getenv());
            case "rotate-bot" -> rotateBotClientSecret(store, System.getenv());
            default -> throw new IllegalArgumentException("Choose public, bot, or rotate-bot");
        }
    }

    private static void registerPublicClient(RegisteredClientRepository repository, String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "A public client requires a client ID and at least one exact redirect URI");
        }
        String clientId = args[1];
        validateClientId(clientId);
        List<String> redirectUris = Arrays.stream(args, 2, args.length)
                .peek(OAuthClientRegistrationTool::validatePublicRedirectUri)
                .distinct()
                .toList();
        ensureClientIdAvailable(repository, clientId);

        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName("Port Royal public client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(uris -> uris.addAll(redirectUris))
                .scope("openid")
                .scope("profile")
                .scope("game:play")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(tokenSettings())
                .build();
        repository.save(client);
        System.out.println("Registered the public OAuth client. No client secret was created.");
    }

    private static void registerBotClient(RegisteredClientRepository repository, java.util.Map<String, String> environment) {
        String clientId = environment.getOrDefault("OAUTH2_BOT_CLIENT_ID", "");
        String clientSecret = environment.getOrDefault("OAUTH2_BOT_CLIENT_SECRET", "");
        validateClientId(clientId);
        if (clientSecret.length() < 32) {
            throw new IllegalArgumentException("OAUTH2_BOT_CLIENT_SECRET must contain at least 32 characters");
        }
        ensureClientIdAvailable(repository, clientId);

        repository.save(botClient(UUID.randomUUID().toString(), clientId, clientSecret));
        System.out.println("Registered the confidential Telegram OAuth client.");
    }

    private static void rotateBotClientSecret(ClientStore store, java.util.Map<String, String> environment) {
        String clientId = environment.getOrDefault("OAUTH2_BOT_CLIENT_ID", "");
        String clientSecret = environment.getOrDefault("OAUTH2_BOT_CLIENT_SECRET", "");
        validateClientId(clientId);
        if (clientSecret.length() < 32) {
            throw new IllegalArgumentException("OAUTH2_BOT_CLIENT_SECRET must contain at least 32 characters");
        }
        RegisteredClient existing = store.registeredClientRepository().findByClientId(clientId);
        if (existing == null
                || !existing.getAuthorizationGrantTypes().contains(TelegramDelegationGrantAuthenticationToken.GRANT_TYPE)) {
            throw new IllegalArgumentException("The Telegram OAuth client is not registered");
        }

        TransactionTemplate transaction = new TransactionTemplate(
                new DataSourceTransactionManager(store.jdbcTemplate().getDataSource()));
        transaction.executeWithoutResult(status -> {
            store.registeredClientRepository().save(botClient(existing.getId(), clientId, clientSecret));
            store.jdbcTemplate().update(
                    "DELETE FROM oauth2_authorization WHERE registered_client_id = ?", existing.getId());
            store.jdbcTemplate().update(
                    "DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", existing.getId());
        });
        System.out.println("Rotated the confidential Telegram OAuth client and revoked its existing tokens.");
    }

    private static RegisteredClient botClient(String id, String clientId, String clientSecret) {
        return RegisteredClient.withId(id)
                .clientId(clientId)
                .clientName("Port Royal Telegram BOT")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientSecret(new BCryptPasswordEncoder().encode(clientSecret))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(TelegramDelegationGrantAuthenticationToken.GRANT_TYPE)
                .scope("bot:onboard")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(tokenSettings())
                .build();
    }

    static void validatePublicRedirectUri(String value) {
        if (value == null || value.isBlank() || value.contains("*")) {
            throw new IllegalArgumentException("Redirect URIs must be non-empty exact values");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Redirect URI is malformed", exception);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!uri.isAbsolute() || uri.getFragment() != null || uri.getUserInfo() != null
                || scheme.equals("javascript") || scheme.equals("data") || scheme.equals("file")) {
            throw new IllegalArgumentException("Redirect URI must be absolute and must not contain credentials or fragments");
        }
        if (scheme.equals("https")) {
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("HTTPS redirect URI must include a host");
            }
        } else if (scheme.equals("http")) {
            String host = uri.getHost();
            if (!"localhost".equalsIgnoreCase(host)
                    && !"127.0.0.1".equals(host)
                    && !"::1".equals(host)
                    && !"[::1]".equals(host)) {
                throw new IllegalArgumentException("HTTP redirects are allowed only for loopback clients");
            }
        } else if (!scheme.matches("[a-z][a-z0-9+.-]*")) {
            throw new IllegalArgumentException("Redirect URI scheme is invalid");
        }
    }

    private static void validateClientId(String clientId) {
        if (clientId == null || !CLIENT_ID_PATTERN.matcher(clientId).matches()) {
            throw new IllegalArgumentException("Client ID must use letters, digits, '.', '_' or '-'");
        }
    }

    private static void ensureClientIdAvailable(RegisteredClientRepository repository, String clientId) {
        if (repository.findByClientId(clientId) != null) {
            throw new IllegalArgumentException("Client ID is already registered");
        }
    }

    private static TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(5))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .build();
    }

    private static ClientStore createStore() {
        String url = requiredEnvironmentValue("SPRING_DATASOURCE_URL");
        String username = requiredEnvironmentValue("SPRING_DATASOURCE_USERNAME");
        String password = requiredEnvironmentValue("SPRING_DATASOURCE_PASSWORD");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return new ClientStore(jdbcTemplate, new JdbcRegisteredClientRepository(jdbcTemplate));
    }

    private static String requiredEnvironmentValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value;
    }

    private record ClientStore(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
    }
}
