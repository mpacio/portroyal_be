package com.matteopaciolla.prbe.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public final class PublicRefreshClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    public PublicRefreshClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication)) {
            return null;
        }
        boolean refreshGrant = Boolean.TRUE.equals(clientAuthentication.getAdditionalParameters()
                .get(PublicRefreshClientAuthenticationConverter.MARKER));
        boolean refreshRevocation = Boolean.TRUE.equals(clientAuthentication.getAdditionalParameters()
                .get(PublicRefreshClientAuthenticationConverter.REVOCATION_MARKER));
        if (!refreshGrant && !refreshRevocation) {
            return null;
        }
        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("invalid_client");
        }

        String clientId = clientAuthentication.getPrincipal().toString();
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null
                || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                || !client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)
                || !client.getClientSettings().isRequireProofKey()
                || client.getTokenSettings().isReuseRefreshTokens()) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("invalid_client");
        }
        return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
