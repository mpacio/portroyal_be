package com.matteopaciolla.prbe.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public final class ApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ApiClient() {
    }

    public static Response execute(String baseUrl,
                                  String username,
                                  String password,
                                  String method,
                                  String path,
                                  String body,
                                  Map<String, String> headers,
                                  boolean basicAuthRequired) throws IOException, InterruptedException {
        String target = buildTargetUrl(baseUrl, path);
        String normalizedMethod = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .method(normalizedMethod, body == null || body.isBlank()
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (basicAuthRequired && username != null && !username.isBlank()) {
            String credentials = username + ":" + (password == null ? "" : password);
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + encoded);
        }
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isBlank()) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }
        }
        if (body != null && !body.isBlank() && requestBuilder.build().headers().firstValue("Content-Type").isEmpty()) {
            requestBuilder.header("Content-Type", "application/json");
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.headers().firstValue("X-Reason").orElse(""), response.body());
    }

    private static String buildTargetUrl(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalizedBase = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : baseUrl.trim();
        if (!normalizedBase.endsWith("/")) {
            normalizedBase += "/";
        }
        return normalizedBase + path.replaceFirst("^/", "");
    }

    public static boolean isPublicEndpoint(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        String lower = path.toLowerCase();
        return lower.contains("/public/") || lower.contains("/public") || lower.startsWith("/login") || lower.startsWith("/perform-login");
    }

    public static String prettyPrint(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "<empty response body>";
        }
        try {
            JsonNode jsonNode = MAPPER.readTree(rawBody);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            return rawBody;
        }
    }

    public static final class Response {
        private final int statusCode;
        private final String reason;
        private final String body;

        public Response(int statusCode, String reason, String body) {
            this.statusCode = statusCode;
            this.reason = reason;
            this.body = body;
        }

        public int statusCode() {
            return statusCode;
        }

        public String reason() {
            return reason;
        }

        public String body() {
            return body;
        }
    }
}
