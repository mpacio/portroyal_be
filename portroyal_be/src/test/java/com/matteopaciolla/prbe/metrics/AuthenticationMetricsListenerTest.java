package com.matteopaciolla.prbe.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationMetricsListenerTest {

    @Test
    void recordsAuthenticationOutcomesWithoutIdentityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthenticationMetricsListener listener = new AuthenticationMetricsListener(registry);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("alice", "not-logged");

        listener.onAuthenticationFailure(new AuthenticationFailureBadCredentialsEvent(
                authentication, new BadCredentialsException("invalid")));
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

        assertThat(registry.get("portroyal_authentication_failures").counter().count()).isEqualTo(1);
        assertThat(registry.get("portroyal_authentication_successes").counter().count()).isEqualTo(1);
        assertThat(registry.get("portroyal_authentication_failures").meters().iterator().next().getId().getTags())
                .isEmpty();
    }
}
