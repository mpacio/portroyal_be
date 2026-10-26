package com.matteopaciolla.prbe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.security.CorrelationIdFilter;
import com.matteopaciolla.prbe.security.NetworkRateLimitFilter;
import com.matteopaciolla.prbe.security.RequestBodySizeLimitFilter;
import com.matteopaciolla.prbe.service.RateLimitService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RateLimitConfiguration {

    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<NetworkRateLimitFilter> networkRateLimitFilter(
            RateLimitService rateLimitService,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<NetworkRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new NetworkRateLimitFilter(rateLimitService, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestBodySizeLimitFilter> requestBodySizeLimitFilter(
            @Value("${app.http.max-request-body-bytes:2097152}") int maxRequestBodyBytes,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<RequestBodySizeLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestBodySizeLimitFilter(maxRequestBodyBytes, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        return registration;
    }
}
