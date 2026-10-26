package com.matteopaciolla.prbe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.converter.ErrorResponseMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(ObjectMapper objectMapper, RateLimitInterceptor rateLimitInterceptor) {
        this.objectMapper = objectMapper;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer pathMatchConfigurer) {
        // This is to ensure that the base API path is only matched by controllers annotated with @RestController
        pathMatchConfigurer.addPathPrefix(Paths.BASE_API_PATH, HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ErrorResponseMessageConverter(objectMapper));
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }
}
