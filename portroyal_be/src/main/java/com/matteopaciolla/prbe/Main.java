package com.matteopaciolla.prbe;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SecuritySchemes({
		@SecurityScheme(
				name = "oauth2",
				type = SecuritySchemeType.OAUTH2,
				description = "Supported clients are browser SPAs, native/mobile apps, CLI clients, and one confidential Telegram BOT; generic server-to-server integrations are not supported. Public clients must be registered by an operator with exact redirect URIs and use Authorization Code with PKCE (S256), without client secrets. The Telegram BOT uses client_credentials only for bot:onboard account flows. Its custom urn:portroyal:grant-type:telegram-delegation grant accepts tgId only at the authenticated token-exchange endpoint and issues a short-lived token for the linked human. No password grant is supported.",
				flows = @OAuthFlows(
						authorizationCode = @OAuthFlow(
								authorizationUrl = "/oauth2/authorize",
								tokenUrl = "/oauth2/token",
								scopes = {
										@OAuthScope(name = "openid", description = "OpenID Connect sign-in"),
										@OAuthScope(name = "profile", description = "Basic profile claims"),
										@OAuthScope(name = "game:play", description = "Access to human game and match APIs")
								}),
						clientCredentials = @OAuthFlow(
								tokenUrl = "/oauth2/token",
								scopes = {
										@OAuthScope(name = "bot:onboard", description = "BOT-authorized account onboarding and linking"),
										@OAuthScope(name = "bot:delegate", description = "Custom Telegram delegation grant only; not available through client_credentials")
								}))
		)
})
@OpenAPIDefinition(
		info = @Info(
				title = "Port Royal Back End",
				description = "Port Royal game backend API. The service exposes the game state, match lifecycle, authentication, user management and bot-mediated operations for the Port Royal game. " +
					"Protected JSON API endpoints under /api/v1 require OAuth 2.0 bearer access tokens. Access tokens last five minutes; rotating refresh tokens last 30 days, and persisted authorization revocation is checked on every API request. " +
					"Telegram BOT operations use a confidential OAuth client; delegated tokens represent the linked human account and ordinary game requests do not accept tgId. Public clients are manually registered with exact redirect URIs. JSON request bodies are limited to 2 MiB; application rate limits return HTTP 429 with Retry-After. The API uses path-based major versions and provides at least six months of notice before endpoint retirement. Password reset is available only for accounts with a confirmed email, and credential changes revoke persisted tokens and invalidate old browser sessions. For client setup and flow details, see the client and OAuth operator guides.",
				version = "1.0"
		),
		servers = {
				@Server(url = "http://localhost:8080", description = "Local development environment"),
				@Server(url = "https://prbe.matteopaciolla.com", description = "Production / hosted API")
		}
)
@Slf4j
@SpringBootApplication
@EnableScheduling
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

}
