package com.matteopaciolla.prbe.security.callback;

import com.matteopaciolla.prbe.exceptions.callback.InvalidCallbackUrlException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackUrlValidatorTest {

    private static final String PUBLIC_URL = "https://hooks.example.com/port-royal";

    @Test
    void rejectsNonHttpsSchemesAndUnsupportedPorts() {
        CallbackUrlValidator validator = validatorResolvingTo(address("8.8.8.8"));

        assertInvalid(validator, "http://hooks.example.com/callback");
        assertInvalid(validator, "ftp://hooks.example.com/callback");
        assertInvalid(validator, "https://hooks.example.com:8443/callback");
    }

    @Test
    void rejectsUserInfoAndFragments() {
        CallbackUrlValidator validator = validatorResolvingTo(address("8.8.8.8"));

        assertInvalid(validator, "https://user:password@hooks.example.com/callback");
        assertInvalid(validator, "https://hooks.example.com/callback#fragment");
    }

    @Test
    void rejectsLocalhostNamesAndIpLiterals() {
        CallbackUrlValidator validator = validatorResolvingTo(address("8.8.8.8"));

        assertInvalid(validator, "https://localhost/callback");
        assertInvalid(validator, "https://service.localhost/callback");
        assertInvalid(validator, "https://127.0.0.1/callback");
        assertInvalid(validator, "https://[::1]/callback");
    }

    @Test
    void rejectsLoopbackPrivateAndLinkLocalDnsAnswers() {
        assertInvalid(validatorResolvingTo(address("127.0.0.1")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("10.10.0.2")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("172.20.0.1")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("192.168.1.1")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("169.254.169.254")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("fe80::1")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("fc00::1")), PUBLIC_URL);
    }

    @Test
    void rejectsReservedAndCloudPlatformAddresses() {
        assertInvalid(validatorResolvingTo(address("192.0.2.1")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("168.63.129.16")), PUBLIC_URL);
        assertInvalid(validatorResolvingTo(address("2001:db8::1")), PUBLIC_URL);
    }

    @Test
    void rejectsDnsResolutionFailuresAndEmptyAnswers() {
        CallbackUrlValidator failingResolver = new CallbackUrlValidator(host -> {
            throw new UnknownHostException("unresolvable");
        });
        CallbackUrlValidator emptyResolver = new CallbackUrlValidator(host -> new InetAddress[0]);

        assertInvalid(failingResolver, PUBLIC_URL);
        assertInvalid(emptyResolver, PUBLIC_URL);
    }

    @Test
    void rejectsWhenAnyDnsAnswerIsDisallowed() {
        CallbackUrlValidator validator = new CallbackUrlValidator(
                host -> new InetAddress[]{address("8.8.8.8"), address("10.0.0.5")});

        assertInvalid(validator, PUBLIC_URL);
    }

    @Test
    void acceptsPublicHttpsDestination() {
        CallbackUrlValidator validator = validatorResolvingTo(address("8.8.8.8"));

        assertThatCode(() -> validator.validate(PUBLIC_URL)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("https://hooks.example.com:443/callback"))
                .doesNotThrowAnyException();
    }

    private static CallbackUrlValidator validatorResolvingTo(InetAddress address) {
        return new CallbackUrlValidator(host -> new InetAddress[]{address});
    }

    private static InetAddress address(String value) {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    private static void assertInvalid(CallbackUrlValidator validator, String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(InvalidCallbackUrlException.class);
    }
}
