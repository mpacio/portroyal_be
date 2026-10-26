package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import com.matteopaciolla.prbe.security.TelegramDelegationGrantAuthenticationConverter;
import com.matteopaciolla.prbe.security.TelegramDelegationGrantAuthenticationProvider;
import com.matteopaciolla.prbe.security.TelegramDelegationGrantAuthenticationToken;
import com.matteopaciolla.prbe.security.TelegramIdentityBoundaryFilter;
import com.matteopaciolla.prbe.service.OAuth2AuthorizationRevocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class OAuth2SecurityConfiguration {

    private static final String API_AUDIENCE = "portroyal-api";
    private static final String GAME_SCOPE = "game:play";
    private static final String BOT_ONBOARD_SCOPE = "bot:onboard";

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserRepository userRepository,
            OAuth2AuthorizationRevocationService revocationService,
            @Value("${app.oauth2.bot.client-id:}") String botClientId) throws Exception {
        TelegramDelegationGrantAuthenticationProvider telegramGrantProvider =
                new TelegramDelegationGrantAuthenticationProvider(
                        authorizationService, tokenGenerator, userRepository, botClientId);
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();
        AuthenticationSuccessHandler oidcLogoutHandler = (request, response, authentication) -> {
            if (authentication instanceof OidcLogoutAuthenticationToken logoutAuthentication) {
                Object principal = logoutAuthentication.getPrincipal();
                if (principal instanceof Authentication authenticatedPrincipal
                        && authenticatedPrincipal.isAuthenticated()) {
                    revocationService.revokePrincipal(authenticatedPrincipal.getName());
                } else if (logoutAuthentication.getIdToken() != null) {
                    revocationService.revokePrincipalByUserId(logoutAuthentication.getIdToken().getSubject());
                }
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                String redirectUri = logoutAuthentication.getPostLogoutRedirectUri();
                if (redirectUri != null) {
                    UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(redirectUri);
                    if (logoutAuthentication.getState() != null) {
                        redirect.queryParam("state", logoutAuthentication.getState());
                    }
                    response.sendRedirect(redirect.build().encode().toUriString());
                    return;
                }
                response.setStatus(HttpStatus.OK.value());
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("You have been signed out.");
            }
        };
        authorizationServerConfigurer
                .authorizationEndpoint(authorization -> authorization.consentPage("/oauth2/consent"))
                .oidc(oidc -> oidc.logoutEndpoint(logout ->
                        logout.logoutResponseHandler(oidcLogoutHandler)))
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                        .accessTokenRequestConverters(converters ->
                                converters.add(0, new TelegramDelegationGrantAuthenticationConverter()))
                        .authenticationProviders(providers -> providers.add(telegramGrantProvider)));

        http.securityMatcher(endpointsMatcher)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        new AntPathRequestMatcher("/oauth2/token", "POST"),
                        new AntPathRequestMatcher("/oauth2/revoke", "POST"),
                        new AntPathRequestMatcher("/oauth2/introspect", "POST")))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .apply(authorizationServerConfigurer);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            @Qualifier("apiJwtDecoder") JwtDecoder apiJwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.securityMatcher(Paths.BASE_API_PATH + "/**", "/actuator/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(Paths.BASE_API_PATH + Paths.PUBLIC_PATH + "/**").permitAll()
                        .requestMatchers("/swagger-ui/**",
                                Paths.BASE_API_PATH + "/api-docs/**",
                                Paths.BASE_API_PATH + "/api-docs.yaml").permitAll()
                        .requestMatchers(Paths.BASE_API_PATH + Paths.USER_PATH + "/register/telegram")
                            .hasAuthority("SCOPE_" + BOT_ONBOARD_SCOPE)
                        .requestMatchers(Paths.BASE_API_PATH + Paths.USER_PATH + "/unify")
                            .hasAuthority("SCOPE_" + BOT_ONBOARD_SCOPE)
                        .requestMatchers(Paths.BASE_API_PATH + Paths.USER_PATH + "/new-tg-unify-email-otp")
                            .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN", "SCOPE_" + BOT_ONBOARD_SCOPE)
                        .requestMatchers(Paths.BASE_API_PATH + "/**")
                            .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .anyRequest().denyAll())
                .addFilterBefore(new TelegramIdentityBoundaryFilter(objectMapper),
                        BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(apiJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRevocationService revocationService) throws Exception {
        LogoutHandler revokeAuthorizations = (request, response, authentication) -> {
            if (authentication != null && authentication.getName() != null) {
                revocationService.revokePrincipal(authentication.getName());
            }
        };
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/*", "/css/**", "/js/**", "/login",
                                Paths.PUBLIC_PATH + "/confirm-email", "/swagger-ui/**").permitAll()
                        .requestMatchers("/oauth2/consent").authenticated()
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .defaultSuccessUrl("/")
                        .failureUrl("/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .addLogoutHandler(revokeAuthorizations)
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .permitAll());
        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${app.oauth2.issuer:}") String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("OAUTH2_ISSUER must be configured");
        }
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            UserRepository userRepository,
            @Value("${app.oauth2.bot.client-id:}") String botClientId) {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                if (context.getTokenType().getValue().equals("id_token")) {
                    userRepository.findByUsername(context.getPrincipal().getName()).ifPresent(user ->
                            context.getClaims()
                                    .subject(user.getId().toString())
                                    .claim("preferred_username", user.getUsername()));
                }
                return;
            }

            context.getClaims()
                    .id(UUID.randomUUID().toString())
                    .audience(new ArrayList<>(List.of(API_AUDIENCE)))
                    .claim("client_id", context.getRegisteredClient().getClientId());
            if (context.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientAuthentication) {
                String clientId = clientAuthentication.getRegisteredClient().getClientId();
                context.getClaims().subject(clientId);
                if (clientId.equals(botClientId)
                        && context.getAuthorizedScopes().contains(BOT_ONBOARD_SCOPE)) {
                    context.getClaims().claim("roles", new ArrayList<>(List.of("ROLE_BOT_CLIENT")));
                } else {
                    context.getClaims().claim("roles", new ArrayList<>());
                }
                return;
            }

            userRepository.findByUsername(context.getPrincipal().getName()).ifPresent(user -> {
                context.getClaims()
                        .subject(user.getId().toString())
                        .claim("username", user.getUsername())
                        .claim("roles", new ArrayList<>(user.getRoles().stream()
                                .filter(role -> role == UserRole.USER || role == UserRole.ADMIN)
                                .map(UserRole::toString)
                                .toList()));
                if (context.getAuthorizationGrant() instanceof TelegramDelegationGrantAuthenticationToken) {
                    context.getClaims().claim("actor_client_id", context.getRegisteredClient().getClientId());
                }
                if (user.getEmail() != null && user.isEmailConfirmed()) {
                    context.getClaims()
                            .claim("email", user.getEmail())
                            .claim("email_verified", true);
                }
            });
        };
    }

    @Bean
    OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, new OAuth2AccessTokenGenerator(), new OAuth2RefreshTokenGenerator());
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean("apiJwtDecoder")
    JwtDecoder apiJwtDecoder(
            @Qualifier("jwtDecoder") JwtDecoder signatureDecoder,
            OAuth2AuthorizationService authorizationService,
            RegisteredClientRepository registeredClientRepository,
            UserRepository userRepository,
            @Value("${app.oauth2.issuer:}") String issuer) {
        return token -> {
            Jwt jwt = signatureDecoder.decode(token);
            if (issuer == null || !issuer.equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString())) {
                throw new BadJwtException("Invalid token issuer");
            }
            if (!jwt.getAudience().contains(API_AUDIENCE)) {
                throw new BadJwtException("Invalid token audience");
            }
            if (jwt.getId() == null || jwt.getExpiresAt() == null
                    || !jwt.getExpiresAt().isAfter(Instant.now())) {
                throw new BadJwtException("Token is expired or missing required claims");
            }

            OAuth2Authorization authorization =
                    authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            if (authorization == null || authorization.getAccessToken() == null
                    || authorization.getAccessToken().isInvalidated()
                    || !authorization.getAccessToken().getToken().getTokenValue().equals(token)) {
                throw new BadJwtException("Token is revoked or unknown");
            }
            RegisteredClient registeredClient =
                    registeredClientRepository.findById(authorization.getRegisteredClientId());
            if (registeredClient == null
                    || !registeredClient.getClientId().equals(jwt.getClaimAsString("client_id"))) {
                throw new BadJwtException("Invalid token client");
            }

            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())) {
                if (!registeredClient.getClientId().equals(jwt.getSubject())
                        || !authorization.getPrincipalName().equals(registeredClient.getClientId())) {
                    throw new BadJwtException("Invalid client token subject");
                }
            } else {
                UserEntity user = userRepository.findByUsername(authorization.getPrincipalName()).orElse(null);
                if (user == null || !user.isEnabled()
                        || !user.getId().toString().equals(jwt.getSubject())) {
                    throw new BadJwtException("Invalid user token subject");
                }
            }
            return jwt;
        };
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopesConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }
            boolean gameAccess = authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals("SCOPE_" + GAME_SCOPE));
            boolean botOnboarding = authorities.stream()
                    .anyMatch(authority -> authority.getAuthority().equals("SCOPE_" + BOT_ONBOARD_SCOPE));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .filter(role -> (gameAccess && (role.equals("ROLE_USER") || role.equals("ROLE_ADMIN")))
                                || (botOnboarding && role.equals("ROLE_BOT_CLIENT")))
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    @Profile({"dev", "test"})
    JWKSource<SecurityContext> developmentJwkSource() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    @Profile("!dev & !test")
    JWKSource<SecurityContext> productionJwkSource(
            @Value("${app.oauth2.signing-key.path:}") String path,
            @Value("${app.oauth2.signing-key.password:}") String password,
            @Value("${app.oauth2.signing-key.password-file:}") String passwordFile,
            @Value("${app.oauth2.signing-key.alias:}") String alias) throws Exception {
        String keyPassword = password;
        if ((keyPassword == null || keyPassword.isBlank())
                && passwordFile != null && !passwordFile.isBlank()) {
            keyPassword = Files.readString(Path.of(passwordFile)).stripTrailing();
        }
        if (path == null || path.isBlank() || keyPassword == null || keyPassword.isBlank()
                || alias == null || alias.isBlank()) {
            throw new IllegalStateException(
                    "A durable PKCS12 signing key must be configured with OAUTH2_SIGNING_KEY_PATH, "
                            + "a signing-key password or password file, and OAUTH2_SIGNING_KEY_ALIAS");
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
            keyStore.load(inputStream, keyPassword.toCharArray());
        }
        List<JWK> rsaKeys = new ArrayList<>();
        boolean activeKeyLoaded = false;
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String keyAlias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(keyAlias);
            if (!(certificate instanceof X509Certificate x509Certificate)
                    || !(x509Certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
                continue;
            }
            RSAKey.Builder keyBuilder = new RSAKey.Builder(publicKey)
                    .keyID(keyAlias)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256);
            if (keyAlias.equalsIgnoreCase(alias)) {
                Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
                if (!(key instanceof RSAPrivateKey privateKey)) {
                    throw new IllegalStateException(
                            "The configured signing alias must contain an RSA private key and certificate");
                }
                keyBuilder.privateKey(privateKey)
                        .keyOperations(Set.of(KeyOperation.SIGN, KeyOperation.VERIFY));
                activeKeyLoaded = true;
            } else {
                keyBuilder.keyOperations(Set.of(KeyOperation.VERIFY));
            }
            rsaKeys.add(keyBuilder.build());
        }
        if (!activeKeyLoaded) {
            throw new IllegalStateException("The configured signing alias was not found in the PKCS#12 keystore");
        }
        return new ImmutableJWKSet<>(new JWKSet(rsaKeys));
    }
}
