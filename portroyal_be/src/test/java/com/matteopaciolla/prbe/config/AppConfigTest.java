package com.matteopaciolla.prbe.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppConfigTest {

    @Test
    void restTemplateDoesNotFollowRedirects() throws Exception {
        AtomicInteger redirectedRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add(
                    "Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
            assertThatThrownBy(() -> new AppConfig().restTemplate()
                    .postForEntity(url, new HttpEntity<>("payload"), Void.class))
                    .isInstanceOf(RestClientException.class);

            assertThat(redirectedRequests).hasValue(0);
        } finally {
            server.stop(0);
        }
    }
}
