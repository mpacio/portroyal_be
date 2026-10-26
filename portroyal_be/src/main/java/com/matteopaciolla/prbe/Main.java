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

@SecuritySchemes({
		@SecurityScheme(
				name = "oauth2",
				type = SecuritySchemeType.OAUTH2,
				description = "Public clients use Authorization Code with PKCE. The Telegram BOT uses client credentials for onboarding and a custom Telegram delegation grant for human game access.",
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
										@OAuthScope(name = "bot:delegate", description = "Request a Telegram delegated human token")
								}))
		)
})
@OpenAPIDefinition(
		info = @Info(
				title = "Port Royal Back End",
				description = "Port Royal game backend API. The service exposes the game state, match lifecycle, authentication, user management and bot-mediated operations for the Port Royal game. " +
					"Protected JSON API endpoints under /api/v1 require OAuth 2.0 bearer access tokens. " +
					"Telegram BOT operations use a confidential OAuth client; delegated tokens represent the linked human account.",
				version = "1.0"
		),
		servers = {
				@Server(url = "http://localhost:8080", description = "Local development environment"),
				@Server(url = "https://prbe.matteopaciolla.com", description = "Production / hosted API")
		}
)
@Slf4j
@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

}
