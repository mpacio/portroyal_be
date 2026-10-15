package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.security.PublicRefreshClientAuthenticationConverter;
import com.matteopaciolla.prbe.security.PublicRefreshClientAuthenticationProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.Instant;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationService jdbcOAuth2AuthorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            @Qualifier("jdbcOAuth2AuthorizationService") OAuth2AuthorizationService jdbcOAuth2AuthorizationService,
            JdbcTemplate jdbcTemplate) {
        return new com.matteopaciolla.prbe.security.RotatingOAuth2AuthorizationService(
                jdbcOAuth2AuthorizationService, jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            UserRepository userRepository,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .cors(Customizer.withDefaults())
                .with(authorizationServerConfigurer, authorizationServer ->
                        authorizationServer
                                .authorizationService(authorizationService)
                                .clientAuthentication(clientAuthentication -> clientAuthentication
                                        .authenticationConverters(converters -> converters.add(
                                                0, new PublicRefreshClientAuthenticationConverter()))
                                        .authenticationProviders(providers -> providers.add(
                                                0, new PublicRefreshClientAuthenticationProvider(
                                                        registeredClientRepository))))
                                .oidc(oidc -> oidc
                                        .userInfoEndpoint(userInfo -> userInfo.userInfoMapper(
                                                context -> userInfo(context, userRepository)))))
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/oauth2/token"),
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/oauth2/revoke")))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(
            Environment environment,
            ResourceLoader resourceLoader,
            @Value("${pr.auth.key-store-location:}") String keyStoreLocation,
            @Value("${pr.auth.key-store-password:}") String keyStorePassword,
            @Value("${pr.auth.key-alias:portroyal-signing}") String keyAlias,
            @Value("${pr.auth.public-key-aliases:}") String publicKeyAliases) {
        try {
            if (keyStoreLocation == null || keyStoreLocation.isBlank()) {
                if (!environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                    throw new IllegalStateException(
                            "PORTROYAL_AUTH_KEYSTORE must be configured unless the dev or test profile is active.");
                }
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(3072);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                return jwkSource(List.of(new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                        .privateKey((RSAPrivateKey) keyPair.getPrivate())
                        .keyID(keyAlias)
                        .build()));
            }

            Resource keyStoreResource = resourceLoader.getResource(keyStoreLocation);
            char[] password = keyStorePassword.toCharArray();
            try (InputStream inputStream = keyStoreResource.getInputStream()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(inputStream, password);
                Key key = keyStore.getKey(keyAlias, password);
                Certificate certificate = keyStore.getCertificate(keyAlias);
                if (!(key instanceof RSAPrivateKey privateKey)
                        || certificate == null
                        || !(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
                    throw new IllegalStateException("The configured signing-key alias must contain an RSA key pair.");
                }
                List<JWK> jwks = new ArrayList<>();
                jwks.add(new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyAlias).build());
                if (publicKeyAliases != null && !publicKeyAliases.isBlank()) {
                    for (String alias : publicKeyAliases.split(",")) {
                        String publicAlias = alias.trim();
                        if (publicAlias.isBlank() || publicAlias.equals(keyAlias)) {
                            throw new IllegalStateException(
                                    "Public signing-key aliases must be non-empty and differ from the active alias.");
                        }
                        Certificate oldCertificate = keyStore.getCertificate(publicAlias);
                        if (oldCertificate == null || !(oldCertificate.getPublicKey() instanceof RSAPublicKey oldKey)) {
                            throw new IllegalStateException(
                                    "Each public signing-key alias must contain an RSA certificate.");
                        }
                        jwks.add(new RSAKey.Builder(oldKey).keyID(publicAlias).build());
                    }
                }
                return jwkSource(jwks);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Unable to load the OAuth signing key.", exception);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            @Value("${pr.auth.issuer}") String issuer,
            @Value("${pr.auth.api-audience}") String apiAudience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, audience -> audience != null && audience.contains(apiAudience));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> accessTokenCustomizer(
            UserRepository userRepository,
            @Value("${pr.auth.api-audience}") String apiAudience,
            @Value("${pr.auth.bot-client-id}") String botClientId,
            @Value("${pr.auth.bot-username}") String botUsername,
            @Value("${pr.auth.key-alias:portroyal-signing}") String signingKeyAlias) {
        return context -> {
            context.getJwsHeader().keyId(signingKeyAlias);
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                    context.getClaims().audience(new java.util.ArrayList<>(
                            List.of(context.getRegisteredClient().getClientId())));
                }
                return;
            }

            UserEntity user;
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())
                    && botClientId.equals(context.getRegisteredClient().getClientId())) {
                user = userRepository.findByUsername(botUsername)
                        .orElseThrow(() -> new IllegalStateException("The Telegram technical user is not provisioned."));
                if (!user.getRoles().contains(UserRole.BOT)) {
                    throw new IllegalStateException("The Telegram technical user must have the BOT role.");
                }
            } else {
                String username = context.getPrincipal().getName();
                user = userRepository.findByUsernameAndEnabledAndEmailConfirmed(username, true, true)
                        .orElseThrow(() -> new IllegalStateException("The authenticated user is no longer active."));
            }

            context.getClaims()
                    .subject(user.getUsername())
                    .audience(new java.util.ArrayList<>(List.of(apiAudience)))
                    .claim("roles", new java.util.ArrayList<>(
                            user.getRoles().stream().map(UserRole::toString).toList()));
        };
    }

    @Bean
    OAuth2TokenGenerator<?> oauth2TokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> accessTokenCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(accessTokenCustomizer);

        OAuth2RefreshTokenGenerator confidentialRefreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenGenerator = context -> {
            if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
                return null;
            }
            boolean publicAuthorizationCodeClient =
                    AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())
                            && context.getAuthorizationGrant() != null
                            && context.getAuthorizationGrant().getPrincipal()
                            instanceof OAuth2ClientAuthenticationToken clientAuthentication
                            && ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod());
            if (!publicAuthorizationCodeClient) {
                return confidentialRefreshTokenGenerator.generate(context);
            }
            if (!context.getRegisteredClient().getClientSettings().isRequireProofKey()
                    || context.getRegisteredClient().getTokenSettings().isReuseRefreshTokens()
                    || !context.getAuthorizedScopes().contains("offline_access")) {
                return null;
            }
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plus(
                    context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
            byte[] token = new byte[48];
            SecureRandomHolder.INSTANCE.nextBytes(token);
            return new OAuth2RefreshToken(
                    Base64.getUrlEncoder().withoutPadding().encodeToString(token), issuedAt, expiresAt);
        };
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, new OAuth2AccessTokenGenerator(), refreshTokenGenerator);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${pr.auth.issuer}") String issuer,
            Environment environment) {
        java.net.URI issuerUri;
        try {
            issuerUri = java.net.URI.create(issuer);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PORTROYAL_AUTH_ISSUER must be an absolute HTTPS URL.", exception);
        }
        boolean loopbackIssuer = "localhost".equalsIgnoreCase(issuerUri.getHost())
                || "127.0.0.1".equals(issuerUri.getHost())
                || "::1".equals(issuerUri.getHost());
        boolean localProfile = environment.acceptsProfiles(Profiles.of("dev", "test"));
        if (!issuerUri.isAbsolute()
                || issuerUri.getHost() == null
                || issuerUri.getQuery() != null
                || issuerUri.getFragment() != null
                || !("https".equalsIgnoreCase(issuerUri.getScheme())
                || (localProfile && loopbackIssuer && "http".equalsIgnoreCase(issuerUri.getScheme())))) {
            throw new IllegalStateException(
                    "PORTROYAL_AUTH_ISSUER must use HTTPS except for localhost in dev/test.");
        }
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    private static JWKSource<SecurityContext> jwkSource(List<JWK> jwks) {
        if (jwks.isEmpty()) {
            throw new IllegalStateException("At least one OAuth signing public key must be configured.");
        }
        return new ImmutableJWKSet<>(new JWKSet(jwks));
    }

    private static OidcUserInfo userInfo(
            OidcUserInfoAuthenticationContext context,
            UserRepository userRepository) {
        OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
        JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();
        String username = principal.getToken().getSubject();
        UserEntity user = userRepository.findByUsernameAndEnabledAndEmailConfirmed(username, true, true)
                .orElseThrow(() -> new IllegalStateException("The authenticated user is no longer active."));

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("preferred_username", user.getUsername());
        if (user.getFirstName() != null) {
            claims.put("given_name", user.getFirstName());
        }
        if (user.getLastName() != null) {
            claims.put("family_name", user.getLastName());
        }
        if (user.getEmail() != null) {
            claims.put("email", user.getEmail());
            claims.put("email_verified", user.isEmailConfirmed());
        }
        return new OidcUserInfo(claims);
    }

    private static final class SecureRandomHolder {
        private static final SecureRandom INSTANCE = new SecureRandom();
    }
}
