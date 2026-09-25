package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.logging.CustomHttpLogFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.*;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;

import java.util.function.Predicate;

@Configuration
public class LogbookConfig {

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .condition(excludeEndpoints("/health", "/info")) // Example: Exclude specific endpoints
                .sink(new DefaultSink(
                        new CustomHttpLogFormatter(),
                        new DefaultHttpLogWriter()
                ))
                .build();
    }

    private Predicate<HttpRequest> excludeEndpoints(String... endpoints) {
        return request -> {
            for (String endpoint : endpoints) {
                if (request.getPath().equals(endpoint)) {
                    return false;
                }
            }
            return true;
        };
    }
}
