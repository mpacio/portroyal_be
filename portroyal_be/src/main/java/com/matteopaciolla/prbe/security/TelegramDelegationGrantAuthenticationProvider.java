package com.matteopaciolla.prbe.security;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TelegramDelegationGrantAuthenticationProvider implements AuthenticationProvider {

    private static final String GAME_SCOPE = "game:play";
    private static final Pattern TELEGRAM_ID_PATTERN = Pattern.compile("[0-9]{1,20}");

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final UserRepository userRepository;
    private final String botClientId;

    public TelegramDelegationGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserRepository userRepository,
            String botClientId) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
        this.botClientId = botClientId;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        TelegramDelegationGrantAuthenticationToken grant =
                (TelegramDelegationGrantAuthenticationToken) authentication;
        if (!(grant.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientPrincipal)
                || !clientPrincipal.isAuthenticated()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (botClientId.isBlank()
                || !botClientId.equals(registeredClient.getClientId())
                || !registeredClient.getAuthorizationGrantTypes().contains(grant.getGrantType())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        if (grant.getTelegramId() == null
                || !TELEGRAM_ID_PATTERN.matcher(grant.getTelegramId()).matches()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }
        UserEntity user = userRepository.findByTelegramId(grant.getTelegramId())
                .filter(UserEntity::isEnabled)
                .orElseThrow(() -> new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT));
        UsernamePasswordAuthenticationToken userPrincipal =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                        user.getRoles().stream().map(UserRole::toString).map(SimpleGrantedAuthority::new).toList());
        Set<String> authorizedScopes = Set.of(GAME_SCOPE);

        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userPrincipal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(grant.getGrantType())
                .authorizationGrant(grant)
                .build();
        OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);
        if (generatedToken == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedToken.getTokenValue(),
                generatedToken.getIssuedAt(),
                generatedToken.getExpiresAt(),
                authorizedScopes);
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(user.getUsername())
                .authorizationGrantType(grant.getGrantType())
                .authorizedScopes(authorizedScopes)
                .attribute("actor_client_id", registeredClient.getClientId());
        if (generatedToken instanceof ClaimAccessor claimAccessor) {
            authorizationBuilder.token(accessToken,
                    metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            jdbcSafeClaims(claimAccessor.getClaims())));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }
        authorizationService.save(authorizationBuilder.build());

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, null, java.util.Map.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TelegramDelegationGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static Map<String, Object> jdbcSafeClaims(Map<String, Object> claims) {
        Map<String, Object> safeClaims = new LinkedHashMap<>();
        claims.forEach((name, value) -> safeClaims.put(name, jdbcSafeValue(value)));
        return safeClaims;
    }

    private static Object jdbcSafeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safeMap = new LinkedHashMap<>();
            map.forEach((name, nestedValue) ->
                    safeMap.put(String.valueOf(name), jdbcSafeValue(nestedValue)));
            return safeMap;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> safeList = new ArrayList<>();
            collection.forEach(item -> safeList.add(jdbcSafeValue(item)));
            return safeList;
        }
        return value;
    }
}
