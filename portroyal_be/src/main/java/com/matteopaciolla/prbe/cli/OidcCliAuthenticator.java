package com.matteopaciolla.prbe.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class OidcCliAuthenticator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OidcCliAuthenticator() {
    }

    static TokenSet login(String baseUrl) throws IOException, InterruptedException {
        String clientId = setting("PORTROYAL_CLI_CLIENT_ID", "portroyal-cli");
        String redirectUri = setting("PORTROYAL_CLI_REDIRECT_URI", "http://127.0.0.1:8765/callback");
        validateBaseUrl(baseUrl);
        validateLoopbackRedirect(redirectUri);

        JsonNode discovery = discover(baseUrl);
        String issuer = requiredText(discovery, "issuer");
        String authorizationEndpoint = requiredText(discovery, "authorization_endpoint");
        String tokenEndpoint = requiredText(discovery, "token_endpoint");
        String jwksUri = requiredText(discovery, "jwks_uri");

        String verifier = randomUrlSafe(32);
        String challenge = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        String state = randomUrlSafe(32);
        String nonce = randomUrlSafe(32);
        String authorizationUrl = authorizationEndpoint + (authorizationEndpoint.contains("?") ? "&" : "?")
                + "response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("openid profile email offline_access portroyal-api")
                + "&state=" + encode(state)
                + "&nonce=" + encode(nonce)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256";

        String authorizationCode = receiveAuthorizationCode(authorizationUrl, state);
        JsonNode tokens = postForm(tokenEndpoint, Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "redirect_uri", redirectUri,
                "code", authorizationCode,
                "code_verifier", verifier));

        String accessToken = requiredText(tokens, "access_token");
        String refreshToken = requiredText(tokens, "refresh_token");
        validateIdToken(requiredText(tokens, "id_token"), jwksUri, issuer, clientId, nonce);
        long expiresIn = tokens.path("expires_in").asLong(0);
        if (expiresIn <= 0) {
            throw new IOException("The token response did not include a valid access-token lifetime.");
        }
        return new TokenSet(accessToken, refreshToken, Instant.now().plusSeconds(expiresIn));
    }

    static TokenSet refresh(String baseUrl, String refreshToken) throws IOException, InterruptedException {
        JsonNode discovery = discover(baseUrl);
        String tokenEndpoint = requiredText(discovery, "token_endpoint");
        String clientId = setting("PORTROYAL_CLI_CLIENT_ID", "portroyal-cli");
        JsonNode tokens = postForm(tokenEndpoint, Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", refreshToken));
        String accessToken = requiredText(tokens, "access_token");
        String rotatedRefreshToken = requiredText(tokens, "refresh_token");
        long expiresIn = tokens.path("expires_in").asLong(0);
        if (expiresIn <= 0) {
            throw new IOException("The token response did not include a valid access-token lifetime.");
        }
        return new TokenSet(accessToken, rotatedRefreshToken, Instant.now().plusSeconds(expiresIn));
    }

    static void revoke(String baseUrl, String refreshToken) throws IOException, InterruptedException {
        JsonNode discovery = discover(baseUrl);
        String revocationEndpoint = requiredText(discovery, "revocation_endpoint");
        String clientId = setting("PORTROYAL_CLI_CLIENT_ID", "portroyal-cli");
        HttpResponse<String> response = postFormResponse(revocationEndpoint, Map.of(
                "client_id", clientId,
                "token", refreshToken,
                "token_type_hint", "refresh_token"));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OIDC refresh-token revocation failed with HTTP " + response.statusCode() + ".");
        }
    }

    private static JsonNode discover(String baseUrl) throws IOException, InterruptedException {
        String endpoint = baseUrl.replaceAll("/+$", "") + "/.well-known/openid-configuration";
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OIDC discovery failed with HTTP " + response.statusCode() + ".");
        }
        return MAPPER.readTree(response.body());
    }

    private static JsonNode postForm(String endpoint, Map<String, String> values)
            throws IOException, InterruptedException {
        HttpResponse<String> response = postFormResponse(endpoint, values);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OIDC token request failed with HTTP " + response.statusCode() + ".");
        }
        return MAPPER.readTree(response.body());
    }

    private static HttpResponse<String> postFormResponse(String endpoint, Map<String, String> values)
            throws IOException, InterruptedException {
        String body = values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        return HTTP.send(
                HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String receiveAuthorizationCode(String authorizationUrl, String expectedState)
            throws IOException, InterruptedException {
        HttpServer callbackServer;
        try {
            callbackServer = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8765), 0);
        } catch (IOException exception) {
            throw new IOException("Cannot open the registered CLI callback at 127.0.0.1:8765.", exception);
        }
        CountDownLatch callbackReceived = new CountDownLatch(1);
        AtomicReference<String> code = new AtomicReference<>();
        AtomicReference<String> callbackError = new AtomicReference<>();
        callbackServer.createContext("/callback", exchange ->
                handleCallback(exchange, expectedState, code, callbackError, callbackReceived));
        callbackServer.start();
        try {
            openBrowser(authorizationUrl);
            System.out.println("Complete sign-in in the browser; this CLI will continue automatically.");
            if (!callbackReceived.await(5, TimeUnit.MINUTES)) {
                throw new IOException("OIDC sign-in timed out.");
            }
            if (callbackError.get() != null) {
                throw new IOException("OIDC sign-in was not completed successfully.");
            }
            String value = code.get();
            if (value == null || value.isBlank()) {
                throw new IOException("The OIDC callback did not contain an authorization code.");
            }
            return value;
        } finally {
            callbackServer.stop(0);
        }
    }

    private static void handleCallback(
            HttpExchange exchange,
            String expectedState,
            AtomicReference<String> code,
            AtomicReference<String> callbackError,
            CountDownLatch callbackReceived) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String receivedState = query.get("state");
        if (!constantTimeEquals(expectedState, receivedState)) {
            sendCallbackPage(exchange, 400, "Sign-in response rejected. You may close this page.");
            return;
        }
        String error = query.get("error");
        if (error != null) {
            callbackError.set(error);
        } else {
            code.set(query.get("code"));
        }
        sendCallbackPage(exchange, error == null ? 200 : 400,
                error == null ? "Sign-in complete. You may close this page."
                        : "Sign-in was not completed. You may close this page.");
        callbackReceived.countDown();
    }

    private static void sendCallbackPage(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = ("<!doctype html><html><head><meta charset=\"utf-8\"><title>Port Royal CLI</title></head>"
                + "<body><p>" + message + "</p></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'none'; base-uri 'none'; frame-ancestors 'none'");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void validateIdToken(
            String idToken, String jwksUri, String issuer, String clientId, String expectedNonce) throws IOException {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            Jwt jwt = decoder.decode(idToken);
            if (!jwt.getAudience().contains(clientId)
                    || !constantTimeEquals(expectedNonce, jwt.getClaimAsString("nonce"))
                    || jwt.getSubject() == null
                    || jwt.getSubject().isBlank()) {
                throw new IOException("The OIDC ID token did not match this authorization request.");
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("The OIDC ID token could not be validated.", exception);
        }
    }

    private static void openBrowser(String authorizationUrl) throws IOException {
        URI uri = URI.create(authorizationUrl);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
        } else {
            System.out.println("Open this URL to sign in:");
            System.out.println(uri);
        }
    }

    private static void validateBaseUrl(String baseUrl) throws IOException {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
            if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !("https".equalsIgnoreCase(uri.getScheme())
                    || (loopback && "http".equalsIgnoreCase(uri.getScheme())))) {
                throw new IOException("Use HTTPS for the API; plain HTTP is allowed only for loopback development.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("The API base URL is invalid.", exception);
        }
    }

    private static void validateLoopbackRedirect(String redirectUri) throws IOException {
        URI uri = URI.create(redirectUri);
        if (!"http".equalsIgnoreCase(uri.getScheme())
                || !"127.0.0.1".equals(uri.getHost())
                || uri.getPort() != 8765
                || !"/callback".equals(uri.getPath())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IOException("CLI redirect URI must match the registered loopback callback.");
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new HashMap<>();
        if (query == null || query.isBlank()) {
            return values;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator >= 0) {
                values.put(
                        URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return expected != null && actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String requiredText(JsonNode node, String field) throws IOException {
        String value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            throw new IOException("OIDC metadata/token response is missing " + field + ".");
        }
        return value;
    }

    private static String randomUrlSafe(int byteCount) {
        byte[] bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] sha256(byte[] value) throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable.", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    record TokenSet(String accessToken, String refreshToken, Instant expiresAt) {
    }
}
