package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.security.callback.CallbackUrlValidator;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean(destroyMethod = "close")
    CloseableHttpClient callbackHttpClient(CallbackUrlValidator callbackUrlValidator) {
        DnsResolver dnsResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                try {
                    return callbackUrlValidator.resolvePublicAddresses(host);
                } catch (UnknownHostException exception) {
                    throw exception;
                } catch (RuntimeException exception) {
                    UnknownHostException rejectedHost = new UnknownHostException("Callback host was rejected.");
                    rejectedHost.initCause(exception);
                    throw rejectedHost;
                }
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                resolve(host);
                return host;
            }
        };
        return buildCallbackHttpClient(dnsResolver);
    }

    @Bean
    RestTemplate restTemplate(CloseableHttpClient callbackHttpClient) {
        return buildRestTemplate(callbackHttpClient);
    }

    RestTemplate restTemplate() {
        DnsResolver permissiveTestResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return InetAddress.getAllByName(host);
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                return InetAddress.getByName(host).getCanonicalHostName();
            }
        };
        return buildRestTemplate(buildCallbackHttpClient(permissiveTestResolver));
    }

    private static RestTemplate buildRestTemplate(CloseableHttpClient callbackHttpClient) {
        RestTemplate restTemplate = new RestTemplate(
                new HttpComponentsClientHttpRequestFactory(callbackHttpClient));
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                return response.getStatusCode().is3xxRedirection() || super.hasError(response);
            }

            @Override
            protected byte[] getResponseBody(org.springframework.http.client.ClientHttpResponse response) {
                return new byte[0];
            }
        });
        return restTemplate;
    }

    static CloseableHttpClient buildCallbackHttpClient(DnsResolver dnsResolver) {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(5))
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .build();
    }
}
