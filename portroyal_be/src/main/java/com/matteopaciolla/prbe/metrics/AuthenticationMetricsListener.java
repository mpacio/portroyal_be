package com.matteopaciolla.prbe.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationMetricsListener {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        meterRegistry.counter("portroyal_authentication_failures").increment();
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        meterRegistry.counter("portroyal_authentication_successes").increment();
    }
}
