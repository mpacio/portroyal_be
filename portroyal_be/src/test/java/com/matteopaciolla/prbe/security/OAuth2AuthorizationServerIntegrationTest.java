package com.matteopaciolla.prbe.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OAuth2AuthorizationServerIntegrationTest {

    private static final String BOT_CLIENT_ID = "portroyal-test-bot";
    private static final String BOT_CLIENT_SECRET = "test-bot-secret-for-integration-tests";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .followRedirects(Redirect.NEVER)
            .build();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void discoveryEndpointPublishesConfiguredIssuer() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(uri("/.well-known/openid-configuration")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(response.body()).path("issuer").asText())
                .isEqualTo("http://localhost:8080");
    }

    @Test
    void authorizationServerTokenEndpointAllowsOnlyConfiguredBrowserOrigin() throws Exception {
        HttpResponse<String> allowedOriginResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/oauth2/token"))
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(allowedOriginResponse.statusCode()).isEqualTo(200);
        assertThat(allowedOriginResponse.headers().firstValue("Access-Control-Allow-Origin").orElse(""))
                .isEqualTo("http://localhost:3000");

        HttpResponse<String> unconfiguredOriginResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/oauth2/token"))
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(unconfiguredOriginResponse.headers().firstValue("Access-Control-Allow-Origin"))
                .isEmpty();
    }

    @Test
    void botClientCredentialsIssueScopedBearerAcceptedByApiAndBasicIsRejected() throws Exception {
        String form = "grant_type=client_credentials&scope=portroyal-api";
        String basic = Base64.getEncoder().encodeToString(
                (BOT_CLIENT_ID + ":" + BOT_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
        HttpRequest tokenRequest = HttpRequest.newBuilder(uri("/oauth2/token"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> tokenResponse = httpClient.send(
                tokenRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(tokenResponse.statusCode()).isEqualTo(200);
        JsonNode tokenPayload = objectMapper.readTree(tokenResponse.body());
        String accessToken = tokenPayload.path("access_token").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(tokenPayload.path("token_type").asText()).isEqualToIgnoringCase("Bearer");

        HttpRequest apiRequest = HttpRequest.newBuilder(uri("/api/v1/user/me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> apiResponse = httpClient.send(
                apiRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(apiResponse.statusCode()).isEqualTo(200);
        JsonNode currentUser = objectMapper.readTree(apiResponse.body()).path("data");
        assertThat(currentUser.path("username").asText()).isEqualTo("shaslabot");
        assertThat(currentUser.path("roles").toString()).contains("BOT");

        String legacyBasic = Base64.getEncoder().encodeToString(
                "shaslabot:password".getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> basicResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/api/v1/user/me"))
                        .header("Authorization", "Basic " + legacyBasic)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(basicResponse.statusCode()).isEqualTo(401);
        assertThat(basicResponse.headers().firstValue("WWW-Authenticate").orElse(""))
                .startsWith("Bearer");
    }

    @Test
    void authorizationCodePkceIssuesRotatingRefreshTokenAndBearerForHumanUser() throws Exception {
        String username = "alice42";
        String password = "this-is-a-test-passphrase";
        UserEntity user = new UserEntity(username, passwordEncoder.encode(password), List.of(UserRole.USER));
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        userRepository.save(user);

        String clientId = "portroyal-test-cli";
        String redirectUri = "http://127.0.0.1:8765/callback";
        String verifier = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("test-code-verifier-with-more-than-43-characters-123".getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        String state = "test-state-value";
        String nonce = "test-nonce-value";

        String authorizeUrl = "/oauth2/authorize?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("openid profile email offline_access portroyal-api")
                + "&state=" + encode(state)
                + "&nonce=" + encode(nonce)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256";
        HttpResponse<String> authorizeResponse = httpClient.send(
                HttpRequest.newBuilder(uri(authorizeUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(authorizeResponse.statusCode()).isBetween(300, 399);

        HttpResponse<String> loginPage = httpClient.send(
                HttpRequest.newBuilder(uri("/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        Matcher csrfMatcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(loginPage.body());
        assertThat(csrfMatcher.find()).isTrue();
        String csrf = csrfMatcher.group(1);

        String loginForm = form(Map.of(
                "username", username,
                "password", password,
                "_csrf", csrf));
        HttpResponse<String> loginResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/perform-login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(loginForm))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode())
                .as("OIDC login form was rejected")
                .isBetween(300, 399);
        String authorizationRedirect = loginResponse.headers().firstValue("Location").orElseThrow();
        URI resumedAuthorizationUri = uri(authorizationRedirect);
        HttpResponse<String> codeResponse = httpClient.send(
                HttpRequest.newBuilder(resumedAuthorizationUri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(codeResponse.statusCode()).isBetween(300, 399);
        URI callback = URI.create(codeResponse.headers().firstValue("Location").orElseThrow());
        assertThat(callback.getHost()).isEqualTo("127.0.0.1");
        var callbackQuery = parseQuery(callback.getRawQuery());
        assertThat(callbackQuery.get("state")).isEqualTo(state);
        String authorizationCode = callbackQuery.get("code");
        assertThat(authorizationCode).isNotBlank();

        JsonNode tokens = tokenForm(Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "redirect_uri", redirectUri,
                "code", authorizationCode,
                "code_verifier", verifier));
        String accessToken = tokens.path("access_token").asText();
        String refreshToken = tokens.path("refresh_token").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).as("Refresh token was not issued for offline_access").isNotBlank();
        assertThat(refreshTokenStatus(refreshToken)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT access_token_metadata FROM oauth2_authorization WHERE principal_name = ?",
                String.class,
                username))
                .doesNotContain("ImmutableCollections");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT oidc_id_token_metadata FROM oauth2_authorization WHERE principal_name = ?",
                String.class,
                username))
                .doesNotContain("ImmutableCollections");

        HttpResponse<String> apiResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/api/v1/user/me"))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(apiResponse.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(apiResponse.body()).path("data").path("username").asText())
                .isEqualTo(username);
        HttpResponse<String> sessionOnlyApiResponse = httpClient.send(
                HttpRequest.newBuilder(uri("/api/v1/user/me"))
                        .header("Cookie", cookieManager.getCookieStore().getCookies().stream()
                                .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                                .map(java.net.HttpCookie::toString)
                                .findFirst()
                                .orElseThrow())
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(sessionOnlyApiResponse.statusCode()).isEqualTo(401);

        JsonNode refreshedTokens = tokenForm(Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", refreshToken));
        assertThat(refreshedTokens.path("access_token").asText()).isNotBlank();
        String rotatedRefreshToken = refreshedTokens.path("refresh_token").asText();
        assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(refreshToken);
        assertThat(refreshTokenStatus(refreshToken)).isEqualTo("USED");
        assertThat(refreshTokenStatus(rotatedRefreshToken)).isEqualTo("ACTIVE");

        HttpResponse<String> reusedTokenResponse = sendTokenForm(Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", refreshToken));
        assertThat(reusedTokenResponse.statusCode()).isBetween(400, 499);
        assertThat(refreshTokenStatus(rotatedRefreshToken)).isEqualTo("REVOKED");

        HttpResponse<String> revokedResponse = sendOAuthForm("/oauth2/revoke", Map.of(
                "client_id", clientId,
                "token", rotatedRefreshToken,
                "token_type_hint", "refresh_token"));
        assertThat(revokedResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> rotatedTokenAfterReplayResponse = sendTokenForm(Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", rotatedRefreshToken));
        assertThat(rotatedTokenAfterReplayResponse.statusCode()).isBetween(400, 499);
    }

    private JsonNode tokenForm(java.util.Map<String, String> form) throws Exception {
        HttpResponse<String> response = sendTokenForm(form);
        assertThat(response.statusCode()).as("OAuth token request failed").isEqualTo(200);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> sendTokenForm(java.util.Map<String, String> values) throws Exception {
        return sendOAuthForm("/oauth2/token", values);
    }

    private HttpResponse<String> sendOAuthForm(String path, java.util.Map<String, String> values) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(values)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static java.util.Map<String, String> parseQuery(String query) {
        return java.util.Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> java.net.URLDecoder.decode(parts.length == 1 ? "" : parts[1], StandardCharsets.UTF_8)));
    }

    private static String form(java.util.Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String refreshTokenStatus(String token) {
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return jdbcTemplate.queryForObject(
                "SELECT status FROM oauth2_refresh_token_history WHERE token_hash = ?",
                String.class,
                hash);
    }

    private URI uri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        return URI.create("http://localhost:" + port + path);
    }
}
