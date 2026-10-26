package com.matteopaciolla.prbe.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthClientRegistrationToolTest {

    @Test
    void acceptsExactHttpsAndLoopbackRedirectUris() {
        assertThatNoException().isThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("https://client.example.com/callback"));
        assertThatNoException().isThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("http://127.0.0.1:49152/callback"));
        assertThatNoException().isThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("com.example.portroyal:/oauth2redirect"));
    }

    @Test
    void rejectsWildcardInsecureRemoteAndFragmentRedirectUris() {
        assertThatThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("https://*.example.com/callback"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("http://client.example.com/callback"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                OAuthClientRegistrationTool.validatePublicRedirectUri("https://client.example.com/callback#fragment"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
